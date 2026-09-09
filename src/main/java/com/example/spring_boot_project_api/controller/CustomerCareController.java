package com.example.spring_boot_project_api.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.cs.AgentNoteRequest;
import com.example.spring_boot_project_api.dto.request.cs.CsChatRequest;
import com.example.spring_boot_project_api.dto.request.cs.HandoffRequest;
import com.example.spring_boot_project_api.dto.request.cs.SupportMessageRequest;
import com.example.spring_boot_project_api.dto.request.cs.UpdateTicketPriorityRequest;
import com.example.spring_boot_project_api.dto.request.cs.UpdateTicketStatusRequest;
import com.example.spring_boot_project_api.dto.response.ai.AIMessageResponse;
import com.example.spring_boot_project_api.dto.response.cs.CsChatResponse;
import com.example.spring_boot_project_api.dto.response.cs.SupportAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.cs.SupportQueueResponse;
import com.example.spring_boot_project_api.dto.response.cs.SupportTicketDetailResponse;
import com.example.spring_boot_project_api.dto.response.cs.SupportTicketNoteResponse;
import com.example.spring_boot_project_api.dto.response.cs.SupportTicketResponse;
import com.example.spring_boot_project_api.enums.TicketStatus;
import com.example.spring_boot_project_api.service.CustomerCareService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cs")
public class CustomerCareController {
    private final CustomerCareService customerCareService;

    public CustomerCareController(CustomerCareService customerCareService) {
        this.customerCareService = customerCareService;
    }

    //Customer chats with the customer-care AI
    @Operation(summary = "Chat with the customer-care AI")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "AI response retrieved successfully")
    })
    @PostMapping("/chat")
    public ResponseEntity<CsChatResponse> chat(
            @RequestParam Long userId,
            @Valid @RequestBody CsChatRequest request) {
        return ResponseEntity.ok(customerCareService.chat(userId, request));
    }

    //One-click handoff from AI to a human agent
    @Operation(summary = "Hand off a conversation to a human agent")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Support ticket created successfully")
    })
    @PostMapping("/handoff")
    public ResponseEntity<SupportTicketResponse> handoff(
            @RequestParam Long userId,
            @Valid @RequestBody HandoffRequest request) {
        return ResponseEntity.ok(customerCareService.handoff(userId, request));
    }

    //Customer's own support requests
    @Operation(summary = "Get all support tickets of a customer")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tickets retrieved successfully")
    })
    @GetMapping("/tickets")
    public ResponseEntity<List<SupportTicketResponse>> getCustomerTickets(
            @RequestParam Long userId) {
        return ResponseEntity.ok(customerCareService.getCustomerTickets(userId));
    }

    //Customer view of one ticket
    @Operation(summary = "Get a customer's ticket with its conversation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ticket retrieved successfully")
    })
    @GetMapping("/tickets/{ticketId}")
    public ResponseEntity<SupportTicketDetailResponse> getCustomerTicket(
            @RequestParam Long userId,
            @PathVariable Long ticketId) {
        return ResponseEntity.ok(customerCareService.getCustomerTicket(userId, ticketId));
    }

    //Customer continues the ticket thread
    @Operation(summary = "Customer sends a message inside a ticket")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Message saved successfully")
    })
    @PostMapping("/tickets/{ticketId}/messages")
    public ResponseEntity<AIMessageResponse> replyAsCustomer(
            @RequestParam Long userId,
            @PathVariable Long ticketId,
            @Valid @RequestBody SupportMessageRequest request) {
        return ResponseEntity.ok(
                customerCareService.replyAsCustomer(userId, ticketId, request.getMessage()));
    }

    //Agent shared inbox + dashboard counts
    @Operation(summary = "Get the customer-care queue with dashboard counts")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Queue retrieved successfully")
    })
    @GetMapping("/queue")
    public ResponseEntity<SupportQueueResponse> getQueue(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(customerCareService.getQueue(status, search));
    }

    //Agent fully-loaded ticket context (internal notes included)
    @Operation(summary = "Get the full agent context for a ticket")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ticket context retrieved successfully")
    })
    @GetMapping("/tickets/{ticketId}/context")
    public ResponseEntity<SupportTicketDetailResponse> getTicketContext(
            @PathVariable Long ticketId) {
        return ResponseEntity.ok(customerCareService.getAgentTicketContext(ticketId));
    }

    //Agent replies inside the customer's conversation
    @Operation(summary = "Agent replies inside the ticket conversation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reply saved successfully")
    })
    @PostMapping("/tickets/{ticketId}/reply")
    public ResponseEntity<AIMessageResponse> replyAsAgent(
            @PathVariable Long ticketId,
            @RequestParam(required = false) Long agentId,
            @Valid @RequestBody SupportMessageRequest request) {
        return ResponseEntity.ok(
                customerCareService.replyAsAgent(ticketId, request.getMessage(), agentId));
    }

    //Agent resolves / updates ticket status
    @Operation(summary = "Update a ticket status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated successfully")
    })
    @PatchMapping("/tickets/{ticketId}/status")
    public ResponseEntity<SupportTicketResponse> updateTicketStatus(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateTicketStatusRequest request) {
        return ResponseEntity.ok(
                customerCareService.updateTicketStatus(ticketId, request.getStatus()));
    }

    //Agent updates ticket priority
    @Operation(summary = "Update a ticket priority")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Priority updated successfully")
    })
    @PatchMapping("/tickets/{ticketId}/priority")
    public ResponseEntity<SupportTicketResponse> updateTicketPriority(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateTicketPriorityRequest request) {
        return ResponseEntity.ok(
                customerCareService.updateTicketPriority(ticketId, request.getPriority()));
    }

    //Agent adds an internal note (never shown to the customer)
    @Operation(summary = "Add an internal note to a ticket")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Note added successfully")
    })
    @PostMapping("/tickets/{ticketId}/notes")
    public ResponseEntity<SupportTicketNoteResponse> addNote(
            @PathVariable Long ticketId,
            @RequestParam(required = false) Long agentId,
            @Valid @RequestBody AgentNoteRequest request) {
        return ResponseEntity.ok(
                customerCareService.addNote(ticketId, request.getNote(), agentId));
    }

    //Customer-care analytics
    @Operation(summary = "Customer-care analytics for a date range")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Analytics retrieved successfully")
    })
    @GetMapping("/analytics")
    public ResponseEntity<SupportAnalyticsResponse> getAnalytics(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(customerCareService.getAnalytics(from, to));
    }
}