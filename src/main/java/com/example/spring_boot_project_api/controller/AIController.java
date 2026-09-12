package com.example.spring_boot_project_api.controller;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.spring_boot_project_api.dto.request.ai.AIChatRequest;
import com.example.spring_boot_project_api.dto.request.ai.RenameConversationRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIChatResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIConversationResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIMessageResponse;
import com.example.spring_boot_project_api.exception.UnauthorizedException;
import com.example.spring_boot_project_api.service.AIService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ai")
public class AIController {
    private final AIService aiService;
    public AIController(AIService aiService){
        this.aiService = aiService;
    }

    private Long currentUserId() {
        return SecurityUtils.currentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
    }

    //Send message and get AI response
    @Operation(summary = "Send a message to the AI assistant")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "AI response retrieved successfully")
    })
    @PostMapping("/chat")
    public ResponseEntity<AIChatResponse> chat(@Valid @RequestBody AIChatRequest request){
        AIChatResponse response = aiService.chat(currentUserId(), request);
        return ResponseEntity.ok(response);
    }

    //Stream AI response token by token over SSE
    @Operation(summary = "Send a message and stream the AI response (Server-Sent Events)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "AI response streamed as text/event-stream")
    })
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid @RequestBody AIChatRequest request){
        Long userId = currentUserId();
        // 3 minute timeout
        SseEmitter emitter = new SseEmitter(300_000L);

        CompletableFuture.runAsync(() -> {
            try {
                AIChatResponse response = aiService.streamChat(userId, request, token -> {
                    try {
                        emitter.send(SseEmitter.event().data(token));
                    } catch (IOException ex) {
                        // client disconnected - keep reading upstream so the reply is still saved
                    }
                });
                // final event so the frontend can sync the saved conversation
                emitter.send(SseEmitter.event().name("done").data(response));
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        });

        return emitter;
    }

    //get all conversations of the authenticated user
    @Operation(summary = "Get the authenticated user's conversations (paginated)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Conversations retrieved successfully")
    })
    @GetMapping("/conversations")
    public ResponseEntity<PagedResponse<AIConversationResponse>> getUserConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size){
        return ResponseEntity.ok(aiService.getUserConversations(currentUserId(), page, size));
    }

    //Get Messages of a conversation
    @Operation(summary = "Get all messages of a conversation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Messages retrieved successfully")
    })
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<AIMessageResponse>> getConversationMessages(@PathVariable Long conversationId){
        List<AIMessageResponse> messages = aiService.getConversationMessages(currentUserId(), conversationId);
        return ResponseEntity.ok(messages);
    }

    @Operation(summary = "Rename a conversation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Conversation renamed successfully")
    })
    @PutMapping("/conversations/{conversationId}")
    public ResponseEntity<AIConversationResponse> updateConversation(
            @PathVariable Long conversationId,
            @RequestBody @Valid RenameConversationRequest request) {

        AIConversationResponse response =
                aiService.updateConversation(currentUserId(), conversationId, request.getTitle());
        return ResponseEntity.ok(response);
    }
    @Operation(summary = "Delete a conversation")
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "Conversation deleted successfully"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Conversation not found"
        )
    })
    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable Long conversationId) {

        aiService.deleteConversation(currentUserId(), conversationId);
        return ResponseEntity.noContent().build();
    }
}