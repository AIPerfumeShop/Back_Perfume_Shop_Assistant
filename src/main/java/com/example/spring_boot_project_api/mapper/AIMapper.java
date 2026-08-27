package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.ai.AIChatRequest;
import com.example.spring_boot_project_api.dto.response.ai.AIConversationResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIMessageResponse;
import com.example.spring_boot_project_api.enums.MessageSender;
import com.example.spring_boot_project_api.model.AIConversation;
import com.example.spring_boot_project_api.model.AIMessage;

@Component
public class AIMapper {
    //AI Conversation
    //AIcon Entity -> AIconResponse (when returning conversation infor to the frontend)
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

    //AI Message
    // AIChatRequest -> AIMessage Entity
    // Used when saving the user's message to the database.

    public AIMessage toMessageEntity(AIChatRequest request, AIConversation conversation){
        if(request == null){
            return null;
        }
        AIMessage message = new AIMessage();
        message.setConversation(conversation);
        message.setSender(MessageSender.USER);
        message.setMessage(request.getMessage());
        return message;
    }

    // AI response -> AIMessage Entity
    // Used when saving the AI's response to the database.
    public AIMessage toMessageEntity(String messageText, MessageSender sender,AIConversation conversation){
        if(messageText == null){
            return null;
        }
        AIMessage message = new AIMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setMessage(messageText);

        return message;
    }
    //AIMessage Entity -> AIMessageResponse (when returning a message to the frontend)
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
