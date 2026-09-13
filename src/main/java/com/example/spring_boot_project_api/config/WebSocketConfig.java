package com.example.spring_boot_project_api.config;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.example.spring_boot_project_api.service.CustomerCareService;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Pattern TICKET_TOPIC =
            Pattern.compile("^/topic/tickets/(\\d+)/messages$");

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomerCareService customerCareService;

    public WebSocketConfig(
            JwtTokenProvider jwtTokenProvider,
            CustomerCareService customerCareService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.customerCareService = customerCareService;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                StompCommand command = accessor.getCommand();
                if (command == null) {
                    return message;
                }
                switch (command) {
                    case CONNECT -> authenticate(accessor);
                    case SUBSCRIBE -> authorizeSubscribe(accessor);
                    default -> {
                    }
                }
                return message;
            }
        });
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String auth = accessor.getFirstNativeHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new MessagingException("Unauthorized");
        }
        String token = auth.substring(7);
        if (!jwtTokenProvider.validate(token) || jwtTokenProvider.isBlacklisted(token)) {
            throw new MessagingException("Unauthorized");
        }
        accessor.setUser(
                new WsUser(jwtTokenProvider.getUserId(token), jwtTokenProvider.getRole(token)));
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (!(principal instanceof WsUser user)) {
            throw new MessagingException("Unauthorized");
        }
        String destination = accessor.getDestination();
        Matcher matcher = destination == null ? null : TICKET_TOPIC.matcher(destination);
        if (matcher == null || !matcher.matches()) {
            throw new MessagingException("Bad destination");
        }
        Long ticketId = Long.valueOf(matcher.group(1));
        if (user.isAdmin()) {
            return;
        }
        customerCareService.isTicketOwner(user.id(), ticketId);
    }
}