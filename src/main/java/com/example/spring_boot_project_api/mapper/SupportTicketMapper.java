package com.example.spring_boot_project_api.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.ai.AIMessageResponse;
import com.example.spring_boot_project_api.dto.response.cs.SupportTicketDetailResponse;
import com.example.spring_boot_project_api.dto.response.cs.SupportTicketNoteResponse;
import com.example.spring_boot_project_api.dto.response.cs.SupportTicketResponse;
import com.example.spring_boot_project_api.dto.response.order.OrderResponse;
import com.example.spring_boot_project_api.model.SupportTicket;
import com.example.spring_boot_project_api.model.SupportTicketNote;

@Component
public class SupportTicketMapper {

    public SupportTicketResponse toResponse(SupportTicket ticket) {
        if (ticket == null) {
            return null;
        }
        SupportTicketResponse response = new SupportTicketResponse();
        response.setId(ticket.getId());
        response.setTicketNumber(ticket.getTicketNumber());
        if (ticket.getUser() != null) {
            response.setUserId(ticket.getUser().getId());
            response.setCustomerName(ticket.getUser().getName());
        }
        response.setStatus(ticket.getStatus());
        response.setPriority(ticket.getPriority());
        response.setOrderId(ticket.getOrderId());
        response.setAgentId(ticket.getAgentId());
        response.setAgentName(ticket.getAgentName());
        response.setSummary(ticket.getSummary());
        response.setReason(ticket.getReason());
        response.setFirstRepliedAt(ticket.getFirstRepliedAt());
        response.setResolvedAt(ticket.getResolvedAt());
        response.setCreatedAt(ticket.getCreatedAt());
        response.setUpdatedAt(ticket.getUpdatedAt());
        return response;
    }

    public List<SupportTicketResponse> toResponseList(List<SupportTicket> tickets) {
        return tickets.stream()
                .map(this::toResponse)
                .toList();
    }

    public SupportTicketDetailResponse toDetailResponse(SupportTicket ticket,
                                                       OrderResponse order,
                                                       List<AIMessageResponse> messages,
                                                       List<SupportTicketNoteResponse> notes) {
        if (ticket == null) {
            return null;
        }
        SupportTicketDetailResponse response = new SupportTicketDetailResponse();
        response.setId(ticket.getId());
        response.setTicketNumber(ticket.getTicketNumber());
        if (ticket.getUser() != null) {
            response.setUserId(ticket.getUser().getId());
            response.setCustomerName(ticket.getUser().getName());
            response.setCustomerEmail(ticket.getUser().getEmail());
            response.setCustomerPhone(ticket.getUser().getPhone());
        }
        response.setStatus(ticket.getStatus());
        response.setPriority(ticket.getPriority());
        response.setOrderId(ticket.getOrderId());
        response.setAgentId(ticket.getAgentId());
        response.setAgentName(ticket.getAgentName());
        response.setSummary(ticket.getSummary());
        response.setReason(ticket.getReason());
        if (ticket.getConversation() != null) {
            response.setConversationId(ticket.getConversation().getId());
        }
        response.setOrder(order);
        response.setMessages(messages);
        response.setNotes(notes);
        response.setFirstRepliedAt(ticket.getFirstRepliedAt());
        response.setResolvedAt(ticket.getResolvedAt());
        response.setCreatedAt(ticket.getCreatedAt());
        response.setUpdatedAt(ticket.getUpdatedAt());
        return response;
    }

    public SupportTicketNoteResponse toNoteResponse(SupportTicketNote note) {
        if (note == null) {
            return null;
        }
        SupportTicketNoteResponse response = new SupportTicketNoteResponse();
        response.setId(note.getId());
        response.setAuthorName(note.getAuthorName());
        response.setContent(note.getContent());
        response.setCreatedAt(note.getCreatedAt());
        return response;
    }

    public List<SupportTicketNoteResponse> toNoteResponseList(List<SupportTicketNote> notes) {
        return notes.stream()
                .map(this::toNoteResponse)
                .toList();
    }
}