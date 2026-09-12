package com.example.spring_boot_project_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.spring_boot_project_api.dto.request.ai.AIChatRequest;
import com.example.spring_boot_project_api.dto.response.ai.AIChatResponse;
import com.example.spring_boot_project_api.enums.MessageSender;
import com.example.spring_boot_project_api.exception.AIServiceException;
import com.example.spring_boot_project_api.exception.ForbiddenException;
import com.example.spring_boot_project_api.mapper.AIMapper;
import com.example.spring_boot_project_api.model.AIConversation;
import com.example.spring_boot_project_api.model.AIMessage;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.AIConversationRepository;
import com.example.spring_boot_project_api.repository.AIMessageRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.impl.AIServiceImpl;

@ExtendWith(MockitoExtension.class)
class AIServiceImplTest {

    @Mock
    private AIConversationRepository aiConversationRepository;

    @Mock
    private AIMessageRepository aiMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AIMapper aiMapper;

    @Mock
    private OpenRouterService openRouterService;

    @InjectMocks
    private AIServiceImpl aiService;

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("Alice");
        return user;
    }

    private AIChatRequest chatRequest(String message) {
        AIChatRequest request = new AIChatRequest();
        request.setMessage(message);
        return request;
    }

    @Test
    void chat_newConversation_savesMessagesAndReturnsReply() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(aiConversationRepository.save(any(AIConversation.class)))
                .thenAnswer(inv -> {
                    AIConversation c = inv.getArgument(0);
                    c.setId(1L);
                    return c;
                });

        AIMessage userMsg = new AIMessage();
        userMsg.setId(11L);
        userMsg.setMessage("Hi");
        userMsg.setSender(MessageSender.USER);
        when(aiMapper.toMessageEntity(any(AIChatRequest.class), any(AIConversation.class)))
                .thenReturn(userMsg);
        when(aiMessageRepository.findByConversationIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(userMsg));
        when(openRouterService.generateResponse(any())).thenReturn("Hello");

        AIMessage aiMsg = new AIMessage();
        aiMsg.setId(22L);
        aiMsg.setMessage("Hello");
        aiMsg.setSender(MessageSender.AI);
        when(aiMapper.toMessageEntity(eq("Hello"), eq(MessageSender.AI), any(AIConversation.class)))
                .thenReturn(aiMsg);
        when(aiMessageRepository.save(any(AIMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        AIChatResponse response = aiService.chat(1L, chatRequest("Hi"));

        assertEquals("Hello", response.getMessage());
        assertEquals(1L, response.getConversationId());
        assertEquals(22L, response.getMessageId());
    }

    @Test
    void chat_existingConversationOwnedByAnotherUser_throwsForbidden() {
        User owner = new User();
        owner.setId(99L);
        AIConversation conversation = new AIConversation();
        conversation.setId(5L);
        conversation.setUser(owner);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(aiConversationRepository.findById(5L)).thenReturn(Optional.of(conversation));

        AIChatRequest request = chatRequest("Hi");
        request.setConversationId(5L);

        assertThrows(ForbiddenException.class, () -> aiService.chat(1L, request));
    }

    @Test
    void streamChat_emitsTokensAndPersistsFullReply() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(aiConversationRepository.save(any(AIConversation.class)))
                .thenAnswer(inv -> {
                    AIConversation c = inv.getArgument(0);
                    c.setId(1L);
                    return c;
                });

        AIMessage userMsg = new AIMessage();
        userMsg.setId(11L);
        userMsg.setMessage("Tell me a perfume");
        userMsg.setSender(MessageSender.USER);
        when(aiMapper.toMessageEntity(any(AIChatRequest.class), any(AIConversation.class)))
                .thenReturn(userMsg);
        when(aiMessageRepository.findByConversationIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(userMsg));

        doAnswer(inv -> {
            Consumer<String> onToken = inv.getArgument(1);
            onToken.accept("Hello");
            onToken.accept(" world");
            return null;
        }).when(openRouterService).streamGenerateResponse(any(), any());

        AIMessage aiMsg = new AIMessage();
        aiMsg.setId(33L);
        aiMsg.setMessage("Hello world");
        aiMsg.setSender(MessageSender.AI);
        when(aiMapper.toMessageEntity(eq("Hello world"), eq(MessageSender.AI), any(AIConversation.class)))
                .thenReturn(aiMsg);
        when(aiMessageRepository.save(any(AIMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        StringBuilder streamed = new StringBuilder();
        AIChatResponse response =
                aiService.streamChat(1L, chatRequest("Tell me a perfume"), streamed::append);

        assertEquals("Hello world", streamed.toString());
        assertEquals("Hello world", response.getMessage());
        assertEquals(33L, response.getMessageId());
    }

    @Test
    void streamChat_emptyUpstreamResponse_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(aiConversationRepository.save(any(AIConversation.class)))
                .thenAnswer(inv -> {
                    AIConversation c = inv.getArgument(0);
                    c.setId(1L);
                    return c;
                });

        AIMessage userMsg = new AIMessage();
        userMsg.setId(11L);
        userMsg.setMessage("Hi");
        when(aiMapper.toMessageEntity(any(AIChatRequest.class), any(AIConversation.class)))
                .thenReturn(userMsg);
        when(aiMessageRepository.findByConversationIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(userMsg));
        doAnswer(inv -> null).when(openRouterService).streamGenerateResponse(any(), any());

        assertThrows(AIServiceException.class, () ->
                aiService.streamChat(1L, chatRequest("Hi"), token -> { }));
    }
}