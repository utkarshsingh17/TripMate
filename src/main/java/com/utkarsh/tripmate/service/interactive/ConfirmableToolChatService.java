package com.utkarsh.tripmate.service.interactive;

import com.utkarsh.tripmate.config.properties.RagProperties;
import com.utkarsh.tripmate.service.RagService;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConfirmableToolChatService {

    private static final Logger log = LoggerFactory.getLogger(ConfirmableToolChatService.class);

    private static final String SYSTEM_MESSAGE =
            "Use all available tools to answer the request. If confirmation is required, ask before executing tools.";

    private final ChatClient openAiChatClient;
    private final ChatClient anthropicChatClient;
    private final ChatClient deepSeekChatClient;
    private final RagService ragService;
    private final RagProperties ragProperties;

    public ConfirmableToolChatService(
            @Qualifier("openAiChatClient") ChatClient openAiChatClient,
            @Qualifier("anthropicChatClient") ChatClient anthropicChatClient,
            @Qualifier("deepSeekChatClient") ChatClient deepSeekChatClient,
            RagService ragService,
            RagProperties ragProperties) {
        this.openAiChatClient = openAiChatClient;
        this.anthropicChatClient = anthropicChatClient;
        this.deepSeekChatClient = deepSeekChatClient;
        this.ragService = ragService;
        this.ragProperties = ragProperties;
    }

    public String chat(String userMessage, int userSuppliedTopK, String modelName, String temperature) throws Exception {
        List<ToolCallback> filteredToolCallbacks;
        try {
            filteredToolCallbacks = ragService.getRagCandidateToolCallbackList(
                    userMessage,
                    userSuppliedTopK <= 0 ? ragProperties.getTopK() : userSuppliedTopK);
        } catch (Exception e) {
            log.warn("RAG filtering failed, falling back to full tool list: {}", e.getMessage());
            filteredToolCallbacks = ragService.getAvailableToolCallbackList();
        }

        if (filteredToolCallbacks == null || filteredToolCallbacks.isEmpty()) {
            filteredToolCallbacks = ragService.getAvailableToolCallbackList();
        }

        ChatResponse response;
        ModelProvider provider = determineModelProvider(modelName);
        if (provider == ModelProvider.OPEN_AI) {
            double temp = modelName != null && modelName.toLowerCase().startsWith("gpt-5") ? 1.0 : Double.parseDouble(temperature);
            response = openAiChatClient.prompt()
                    .user(userMessage)
                    .system(SYSTEM_MESSAGE)
                    .options(OpenAiChatOptions.builder()
                            .model(modelName)
                            .temperature(temp)
                            .internalToolExecutionEnabled(false)
                            .toolCallbacks(filteredToolCallbacks)
                            .build())
                    .call()
                    .chatResponse();
        } else if (provider == ModelProvider.ANTHROPIC) {
            response = anthropicChatClient.prompt()
                    .user(userMessage)
                    .system(SYSTEM_MESSAGE)
                    .options(AnthropicChatOptions.builder()
                            .model(modelName)
                            .temperature(Double.parseDouble(temperature))
                            .internalToolExecutionEnabled(false)
                            .toolCallbacks(filteredToolCallbacks)
                            .build())
                    .call()
                    .chatResponse();
        } else {
            response = deepSeekChatClient.prompt()
                    .user(userMessage)
                    .system(SYSTEM_MESSAGE)
                    .options(DeepSeekChatOptions.builder()
                            .model(modelName)
                            .temperature(Double.parseDouble(temperature))
                            .internalToolExecutionEnabled(false)
                            .toolCallbacks(filteredToolCallbacks)
                            .build())
                    .call()
                    .chatResponse();
        }

        if (response == null || response.getResults() == null || response.getResults().isEmpty()
                || response.getResults().get(0).getOutput() == null) {
            return "";
        }
        return response.getResults().get(0).getOutput().getText();
    }

    public String confirmTool(String conversationId, boolean approved, String feedback, String modelName) throws Exception {
        ModelProvider provider = determineModelProvider(modelName);

        if (provider == ModelProvider.OPEN_AI) {
            return openAiChatClient.prompt()
                    .user("User response")
                    .system(SYSTEM_MESSAGE)
                    .advisors(a -> a.param("conversationId", conversationId)
                            .param("approved", approved)
                            .param("feedback", feedback == null ? "none" : feedback))
                    .options(OpenAiChatOptions.builder()
                            .model(modelName)
                            .internalToolExecutionEnabled(false)
                            .build())
                    .call()
                    .content();
        } else if (provider == ModelProvider.ANTHROPIC) {
            return anthropicChatClient.prompt()
                    .user("User response")
                    .system(SYSTEM_MESSAGE)
                    .advisors(a -> a.param("conversationId", conversationId)
                            .param("approved", approved)
                            .param("feedback", feedback == null ? "none" : feedback))
                    .options(AnthropicChatOptions.builder()
                            .model(modelName)
                            .internalToolExecutionEnabled(false)
                            .build())
                    .call()
                    .content();
        } else {
            return deepSeekChatClient.prompt()
                    .user("User response")
                    .system(SYSTEM_MESSAGE)
                    .advisors(a -> a.param("conversationId", conversationId)
                            .param("approved", approved)
                            .param("feedback", feedback == null ? "none" : feedback))
                    .options(DeepSeekChatOptions.builder()
                            .model(modelName)
                            .internalToolExecutionEnabled(false)
                            .build())
                    .call()
                    .content();
        }
    }

    public List<String> getFilteredToolNamesForPrompt(String userMessage, Integer topK) {
        List<ToolCallback> callbacks;
        try {
            callbacks = ragService.getRagCandidateToolCallbackList(
                    userMessage,
                    topK == null || topK <= 0 ? ragProperties.getTopK() : topK);
        } catch (Exception e) {
            log.warn("Unable to compute filtered tool names via RAG: {}", e.getMessage());
            callbacks = ragService.getAvailableToolCallbackList();
        }
        return callbacks.stream()
                .map(tc -> tc.getToolDefinition().name())
                .collect(Collectors.toList());
    }

    private ModelProvider determineModelProvider(String modelName) throws Exception {
        String normalized = modelName == null ? "" : modelName.toLowerCase();
        if (normalized.startsWith("gpt")) {
            return ModelProvider.OPEN_AI;
        } else if (normalized.startsWith("claude")) {
            return ModelProvider.ANTHROPIC;
        } else if (normalized.startsWith("deepseek")) {
            return ModelProvider.DEEPSEEK;
        }
        throw new IllegalAccessException("ModelName " + modelName + " is not supported");
    }

    enum ModelProvider {
        OPEN_AI, ANTHROPIC, DEEPSEEK
    }
}
