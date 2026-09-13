package com.example.spring_boot_project_api.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import com.example.spring_boot_project_api.config.WsUser;
import com.example.spring_boot_project_api.dto.request.cs.TicketChatMessage;
import com.example.spring_boot_project_api.dto.response.ai.AIMessageResponse;
import com.example.spring_boot_project_api.service.CustomerCareService;
import com.example.spring_boot_project_api.util.RetryUtil;

@Controller
public class TicketChatController {

    private final CustomerCareService customerCareService;
    private final RetryUtil retryUtil;

    public TicketChatController(CustomerCareService customerCareService, RetryUtil retryUtil) {
        this.customerCareService = customerCareService;
        this.retryUtil = retryUtil;
    }

    // Customer or agent sends a chat message on a ticket. The message is
    // persisted and then broadcast live to /topic/tickets/{ticketId}/messages
    // so everyone watching the conversation sees it without refreshing.
    // Each retry re-enters customerCareService through its Spring proxy, so
    // every attempt runs in a fresh transaction.
    @MessageMapping("/tickets/{ticketId}/send")
    @SendTo("/topic/tickets/{ticketId}/messages")
    public AIMessageResponse send(@DestinationVariable Long ticketId,
                                  @Payload TicketChatMessage frame,
                                  Principal principal) {
        WsUser user = (WsUser) principal;
        if (user.isAdmin()) {
            return retryUtil.onDeadlock(
                    () -> customerCareService.replyAsAgent(ticketId, frame.getContent(), user.id()));
        }
        return retryUtil.onDeadlock(
                () -> customerCareService.replyAsCustomer(user.id(), ticketId, frame.getContent()));
    }
}