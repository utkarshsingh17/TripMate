package com.utkarsh.tripmate.service.interactive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utkarsh.tripmate.service.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ToolConfirmationAdvisor implements CallAdvisor {

    private static final Logger log = LoggerFactory.getLogger(ToolConfirmationAdvisor.class);

    private final ConversationStateManager stateManager;
    private final RagService ragService;
    private final int order = 0;

    public ToolConfirmationAdvisor(ConversationStateManager stateManager, RagService ragService) {
        this.stateManager = stateManager;
        this.ragService = ragService;
    }

    @Override
    public String getName() {
        return "ToolConfirmationAdvisor";
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain chain) {
        if (Boolean.TRUE.equals(chatClientRequest.context().get("SKIP_TOOL_CONFIRMATION"))) {
            return chain.nextCall(chatClientRequest);
        }

        String conversationId = extractConversationId(chatClientRequest);
        if (conversationId != null) {
            ConversationState state = stateManager.getState(conversationId);
            if (state != null && state.isPendingConfirmation()) {
                return handlePendingConfirmation(chatClientRequest, state, chain);
            }
        }

        ChatClientResponse response = chain.nextCall(chatClientRequest);
        if (!hasToolCalls(response)) {
            return response;
        }
        return interceptToolCallsForConfirmation(chatClientRequest, response);
    }

    private boolean hasToolCalls(ChatClientResponse response) {
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null || chatResponse.getResults() == null) {
            return false;
        }
        return chatResponse.getResults().stream()
                .anyMatch(gen -> gen.getOutput().getToolCalls() != null && !gen.getOutput().getToolCalls().isEmpty());
    }

    private ChatClientResponse interceptToolCallsForConfirmation(
            ChatClientRequest chatClientRequest,
            ChatClientResponse response) {
        List<AssistantMessage.ToolCall> toolCalls = collectAllToolCalls(response.chatResponse());
        if (toolCalls.isEmpty()) {
            return response;
        }

        ConversationState state = new ConversationState();
        state.setConversationId(UUID.randomUUID().toString());
        state.setOriginalRequest(chatClientRequest);
        state.setOriginalResponse(response);
        state.setToolCalls(toolCalls);
        state.setCurrentToolIndex(0);
        state.setToolResults(new ArrayList<>());
        state.setPendingConfirmation(true);
        stateManager.saveState(state);

        ChatResponse confirmationResponse = createConfirmationResponse(state);
        return new ChatClientResponse(confirmationResponse, response.context());
    }

    private List<AssistantMessage.ToolCall> collectAllToolCalls(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getResults() == null) {
            return Collections.emptyList();
        }
        return chatResponse.getResults().stream()
                .flatMap(g -> g.getOutput().getToolCalls() == null
                        ? Stream.empty()
                        : g.getOutput().getToolCalls().stream())
                .collect(Collectors.toList());
    }

    private ChatClientResponse handlePendingConfirmation(
            ChatClientRequest chatClientRequest,
            ConversationState state,
            CallAdvisorChain chain) {
        boolean approved = extractConfirmationDecision(chatClientRequest);
        AssistantMessage.ToolCall currentTool = state.getToolCalls().get(state.getCurrentToolIndex());

        if (approved) {
            String result = executeToolDirectly(currentTool);
            state.getToolResults().add(new ToolExecutionResult(currentTool, result, true));
        } else {
            String feedback = extractUserFeedback(chatClientRequest);
            state.getToolResults().add(new ToolExecutionResult(
                    currentTool,
                    "Skipped by user" + (feedback == null ? "" : ": " + feedback),
                    false));
        }

        state.setCurrentToolIndex(state.getCurrentToolIndex() + 1);
        if (state.getCurrentToolIndex() < state.getToolCalls().size()) {
            stateManager.updateState(state);
            return new ChatClientResponse(createConfirmationResponse(state), chatClientRequest.context());
        }

        state.setPendingConfirmation(false);
        stateManager.updateState(state);
        return generateFinalResponseWithAdvisor(state, chatClientRequest, chain);
    }

    private String executeToolDirectly(AssistantMessage.ToolCall toolCall) {
        ToolCallback callback = ragService.getAvailableToolCallbackList().stream()
                .filter(tc -> tc.getToolDefinition().name().equals(toolCall.name()))
                .findFirst()
                .orElse(null);
        if (callback == null) {
            return "Tool not found: " + toolCall.name();
        }
        try {
            return callback.call(toolCall.arguments());
        } catch (Exception ex) {
            log.error("Error executing tool {}", toolCall.name(), ex);
            return "Error: " + ex.getMessage();
        }
    }

    private ChatResponse createConfirmationResponse(ConversationState state) {
        AssistantMessage.ToolCall currentTool = state.getToolCalls().get(state.getCurrentToolIndex());
        String messageText = String.format(
                "Tool Confirmation Required%nTool %d of %d: %s%nArguments:%n%s%nDo you want to execute this tool?%n[Conversation ID: %s]",
                state.getCurrentToolIndex() + 1,
                state.getToolCalls().size(),
                currentTool.name(),
                formatArguments(currentTool.arguments()),
                state.getConversationId());

        AssistantMessage message = AssistantMessage.builder().content(messageText).build();
        return new ChatResponse(List.of(new Generation(message)));
    }

    private ChatClientResponse generateFinalResponseWithAdvisor(
            ConversationState state,
            ChatClientRequest chatClientRequest,
            CallAdvisorChain chain) {
        String summary = state.getToolResults().stream()
                .map(r -> String.format("- %s: %s (executed: %s)",
                        r.getToolCall().name(), r.getResult(), r.isExecuted() ? "Y" : "N"))
                .collect(Collectors.joining("\n"));

        String finalPrompt = String.format(
                "Original request: %s%nTool Execution Summary:%n%s%nBased on tool execution results, provide the best response.",
                extractOriginalUserMessage(state.getOriginalRequest()),
                summary);

        ChatClientRequest finalRequest = chatClientRequest.mutate()
                .prompt(new Prompt(finalPrompt))
                .context(Map.of("SKIP_TOOL_CONFIRMATION", true))
                .build();

        stateManager.deleteState(state.getConversationId());
        return chain.nextCall(finalRequest);
    }

    private String extractConversationId(ChatClientRequest request) {
        Object param = request.context().get("conversationId");
        if (param instanceof String id && !id.isBlank()) {
            return id;
        }
        String content = request.prompt().getContents();
        if (content == null) {
            return null;
        }
        String marker = "[Conversation ID:";
        int startIdx = content.indexOf(marker);
        if (startIdx < 0) {
            return null;
        }
        int start = startIdx + marker.length();
        int end = content.indexOf("]", start);
        return end > start ? content.substring(start, end).trim() : null;
    }

    private boolean extractConfirmationDecision(ChatClientRequest request) {
        Object approved = request.context().get("approved");
        if (approved instanceof Boolean b) {
            return b;
        }
        String content = request.prompt().getContents();
        if (content == null) {
            return false;
        }
        String lower = content.toLowerCase();
        return lower.contains("yes") || lower.contains("approve") || lower.contains("confirm");
    }

    private String extractUserFeedback(ChatClientRequest request) {
        Object feedback = request.context().get("feedback");
        return feedback instanceof String s ? s : null;
    }

    private String extractOriginalUserMessage(ChatClientRequest request) {
        if (request == null || request.prompt() == null || request.prompt().getInstructions() == null) {
            return "";
        }
        return request.prompt().getInstructions().stream()
                .filter(m -> m instanceof UserMessage)
                .map(Message::getText)
                .findFirst()
                .orElse("");
    }

    private String formatArguments(String arguments) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Object parsed = mapper.readValue(arguments, Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
        } catch (Exception e) {
            return arguments;
        }
    }
}
