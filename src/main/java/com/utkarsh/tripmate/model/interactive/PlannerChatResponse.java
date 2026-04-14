package com.utkarsh.tripmate.model.interactive;

import java.util.List;

public class PlannerChatResponse {

    private String response;
    private boolean needsConfirmation;
    private String conversationId;
    private String error;
    private List<String> relevantToolList;

    public static PlannerChatResponse buildResponse(String response, List<String> relevantToolList) {
        PlannerChatResponse out = new PlannerChatResponse();
        String conversationId = extractConversationId(response);
        out.setResponse(response);
        out.setNeedsConfirmation(conversationId != null && !conversationId.isBlank());
        out.setConversationId(conversationId);
        out.setRelevantToolList(relevantToolList);
        return out;
    }

    public static PlannerChatResponse error(String error) {
        PlannerChatResponse out = new PlannerChatResponse();
        out.setError(error);
        return out;
    }

    private static String extractConversationId(String response) {
        if (response == null) {
            return null;
        }
        String marker = "[Conversation ID:";
        int startIdx = response.indexOf(marker);
        if (startIdx < 0) {
            return null;
        }
        int start = startIdx + marker.length();
        int end = response.indexOf("]", start);
        if (end <= start) {
            return null;
        }
        return response.substring(start, end).trim();
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public boolean isNeedsConfirmation() {
        return needsConfirmation;
    }

    public void setNeedsConfirmation(boolean needsConfirmation) {
        this.needsConfirmation = needsConfirmation;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public List<String> getRelevantToolList() {
        return relevantToolList;
    }

    public void setRelevantToolList(List<String> relevantToolList) {
        this.relevantToolList = relevantToolList;
    }
}
