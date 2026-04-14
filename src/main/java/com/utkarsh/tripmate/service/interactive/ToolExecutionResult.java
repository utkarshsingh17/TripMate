package com.utkarsh.tripmate.service.interactive;

import org.springframework.ai.chat.messages.AssistantMessage;

public class ToolExecutionResult {

    private final AssistantMessage.ToolCall toolCall;
    private final String result;
    private final boolean executed;

    public ToolExecutionResult(AssistantMessage.ToolCall toolCall, String result, boolean executed) {
        this.toolCall = toolCall;
        this.result = result;
        this.executed = executed;
    }

    public AssistantMessage.ToolCall getToolCall() {
        return toolCall;
    }

    public String getResult() {
        return result;
    }

    public boolean isExecuted() {
        return executed;
    }
}
