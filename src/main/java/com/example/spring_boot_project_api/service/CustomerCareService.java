package com.example.spring_boot_project_api.service;

import java.time.LocalDate;
import java.util.List;

import com.example.spring_boot_project_api.dto.request.cs.CsChatRequest;
import com.example.spring_boot_project_api.dto.request.cs.HandoffRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIMessageResponse;
import com.example.spring_boot_project_api.dto.response.cs.CsChatResponse;
import com.example.spring_boot_project_api.dto.response.cs.SupportAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.cs.SupportQueueResponse;
import com.example.spring_boot_project_api.dto.response.cs.SupportTicketDetailResponse;
import com.example.spring_boot_project_api.dto.response.cs.SupportTicketNoteResponse;
import com.example.spring_boot_project_api.dto.response.cs.SupportTicketResponse;
import com.example.spring_boot_project_api.enums.TicketPriority;
import com.example.spring_boot_project_api.enums.TicketStatus;

public interface CustomerCareService {
    //Customer chats with the customer-care AI
    CsChatResponse chat(Long userId, CsChatRequest request);

    //One-click handoff from AI to a human agent (same conversation, no re-explaining)
    SupportTicketResponse handoff(Long userId, HandoffRequest request);

    //Customer's own tickets
    PagedResponse<SupportTicketResponse> getCustomerTickets(Long userId, int page, int size);

    //Customer view of one ticket (thread, no internal notes)
    SupportTicketDetailResponse getCustomerTicket(Long userId, Long ticketId);

    //Agent fully-loaded context (thread + notes + live order info)
    SupportTicketDetailResponse getAgentTicketContext(Long ticketId);

    //Agent replies inside the customer's conversation
    AIMessageResponse replyAsAgent(Long ticketId, String message, Long agentId);

    //Customer continues the ticket thread (offline "we'll get back to you" flow)
    AIMessageResponse replyAsCustomer(Long userId, Long ticketId, String message);

    SupportTicketResponse updateTicketStatus(Long ticketId, TicketStatus status);

    SupportTicketResponse updateTicketPriority(Long ticketId, TicketPriority priority);

    SupportTicketNoteResponse addNote(Long ticketId, String note, Long agentId);

    //Agent shared inbox + dashboard counts
    SupportQueueResponse getQueue(TicketStatus status, String search);

    SupportAnalyticsResponse getAnalytics(LocalDate from, LocalDate to);
}