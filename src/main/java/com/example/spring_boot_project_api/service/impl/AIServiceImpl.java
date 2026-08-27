package com.example.spring_boot_project_api.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.ai.AIChatRequest;
import com.example.spring_boot_project_api.dto.response.ai.AIChatResponse;
import com.example.spring_boot_project_api.mapper.AIMapper;
import com.example.spring_boot_project_api.model.AIConversation;
import com.example.spring_boot_project_api.model.AIMessage;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.AIConversationRepository;
import com.example.spring_boot_project_api.repository.AIMessageRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.AIService;

@Service
public class AIServiceImpl implements AIService {
    private final AIConversationRepository aiConversationRepository;
    private final AIMessageRepository aiMessageRepository;
    private final UserRepository userRepository;
    private final AIMapper aiMapper;
    public AIServiceImpl(
        AIConversationRepository aiConversationRepository,
        AIMessageRepository aiMessageRepository,
        UserRepository userRepository,
        AIMapper aiMapper){
            this.aiConversationRepository = aiConversationRepository;
            this.aiMessageRepository = aiMessageRepository;
            this.userRepository = userRepository;
            this.aiMapper = aiMapper;
        }
        @Override
        @Transactional
        public AIChatResponse chat(Long userId,AIChatRequest request){
            //Find the user
            User user = userRepository.findById(userId)
            .orElseThrow(()-> new RuntimeException("User not found"));

            //Find existing conversation or create a new one
            AIConversation conversation;
            if(request.getConversationId() == null){
                conversation = new AIConversation();
                conversation.setUser(user);

                //Title can be generated later from the first message
                conversation.setTitle(null);
                conversation = aiConversationRepository.save(conversation);
            }else{
                conversation = aiConversationRepository
                        .findById(request.getConversationId())
                        .orElseThrow(()-> new RuntimeException("Conversation not found"));
                //make sure the conversation belong to the current user
                if(!conversation.getUser().getId().equals(userId)){
                    throw new RuntimeException("You do not have access to this conversation");
                }
            }
            //convert user's request to AImessage
            AIMessage userMessage = aiMapper.toMessageEntity(request, conversation);

            //save user's message
            aiMessageRepository.save(userMessage);
            return null;
        }
}
