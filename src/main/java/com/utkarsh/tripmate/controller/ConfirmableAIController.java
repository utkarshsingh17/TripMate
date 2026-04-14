package com.utkarsh.tripmate.controller;

import com.utkarsh.tripmate.model.interactive.PlannerChatRequest;
import com.utkarsh.tripmate.model.interactive.PlannerChatResponse;
import com.utkarsh.tripmate.model.interactive.ToolConfirmRequest;
import com.utkarsh.tripmate.service.interactive.ConfirmableToolChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class ConfirmableAIController {

    private static final Logger log = LoggerFactory.getLogger(ConfirmableAIController.class);

    private final ConfirmableToolChatService chatService;

    public ConfirmableAIController(ConfirmableToolChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public ResponseEntity<PlannerChatResponse> chat(@RequestBody PlannerChatRequest request) {
        try {
            List<String> filteredToolNames = chatService.getFilteredToolNamesForPrompt(
                    request.getMessage(), request.getUserSuppliedTopK());
            String response = chatService.chat(
                    request.getMessage(),
                    request.getUserSuppliedTopK(),
                    request.getModelName(),
                    request.getTemperature());
            return ResponseEntity.ok(PlannerChatResponse.buildResponse(response, filteredToolNames));
        } catch (Exception e) {
            log.error("Error processing /api/ai/chat", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PlannerChatResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/confirm-tool")
    public ResponseEntity<PlannerChatResponse> confirmTool(@RequestBody ToolConfirmRequest request) {
        try {
            String response = chatService.confirmTool(
                    request.getConversationId(),
                    request.isApproved(),
                    request.getFeedback(),
                    request.getModelName());
            return ResponseEntity.ok(PlannerChatResponse.buildResponse(response, null));
        } catch (Exception e) {
            log.error("Error processing /api/ai/confirm-tool", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PlannerChatResponse.error(e.getMessage()));
        }
    }
}
