package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.ai.AIConversationResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIMessageResponse;
import com.example.spring_boot_project_api.model.AIConversation;
import com.example.spring_boot_project_api.model.AIMessage;

@Component
public class AIMapper {
    public AIConversationResponse toConversationResponse(AIConversation conversation){
        if(conversation == null){
            return null;
        }
        AIConversationResponse response = new AIConversationResponse();
        response.setId(conversation.getId());
        response.setTitle(conversation.getTitle());
        response.setCreatedAt(conversation.getCreatedAt());
        response.setUpdatedAt(conversation.getUpdatedAt());

        if(conversation.getUser() != null){
            response.setUserId(conversation.getUser().getId());
        }
        return response;
    }
    public AIMessageResponse toMessageResponse(AIMessage message){
        if(message == null){
            return null;
        }
        AIMessageResponse response = new AIMessageResponse();
        response.setId(message.getId());
        response.setSender(message.getSender());
        response.setMessage(message.getMessage());
        response.setCreatedAt(message.getCreatedAt());
        response.setUpdatedAt(message.getUpdatedAt());

        return response;
    }
}
