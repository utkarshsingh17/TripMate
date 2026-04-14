package com.utkarsh.tripmate.service.interactive;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.time.Instant;
import java.util.List;

public class ConversationState {

    private String conversationId;
    private ChatClientRequest originalRequest;
    private ChatClientResponse originalResponse;
    private List<AssistantMessage.ToolCall> toolCalls;
    private int currentToolIndex;
    private List<ToolExecutionResult> toolResults;
    private boolean pendingConfirmation;
    private Instant createdAt = Instant.now();

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public ChatClientRequest getOriginalRequest() {
        return originalRequest;
    }

    public void setOriginalRequest(ChatClientRequest originalRequest) {
        this.originalRequest = originalRequest;
    }

    public ChatClientResponse getOriginalResponse() {
        return originalResponse;
    }

    public void setOriginalResponse(ChatClientResponse originalResponse) {
        this.originalResponse = originalResponse;
    }

    public List<AssistantMessage.ToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<AssistantMessage.ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public int getCurrentToolIndex() {
        return currentToolIndex;
    }

    public void setCurrentToolIndex(int currentToolIndex) {
        this.currentToolIndex = currentToolIndex;
    }

    public List<ToolExecutionResult> getToolResults() {
        return toolResults;
    }

    public void setToolResults(List<ToolExecutionResult> toolResults) {
        this.toolResults = toolResults;
    }

    public boolean isPendingConfirmation() {
        return pendingConfirmation;
    }

    public void setPendingConfirmation(boolean pendingConfirmation) {
        this.pendingConfirmation = pendingConfirmation;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
