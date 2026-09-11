package com.example.spring_boot_project_api.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.example.spring_boot_project_api.config.JwtTokenProvider;
import com.example.spring_boot_project_api.config.TelegramBotProperties;
import com.example.spring_boot_project_api.dto.request.auth.TelegramLoginRequest;
import com.example.spring_boot_project_api.dto.response.auth.AuthResponse;
import com.example.spring_boot_project_api.enums.Role;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.UnauthorizedException;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class TelegramAuthServiceImplTest {

    private static final String BOT_TOKEN = "123456:TEST-BOT-TOKEN";
    private static final String CLIENT_ID = "123456";
    private static final long TELEGRAM_ID = 279058397L;

    @Mock
    private TelegramBotProperties properties;
    @Mock
    private JwtDecoder jwtDecoder;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private PasswordEncoder passwordEncoder;

    private TelegramAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TelegramAuthServiceImpl(
                properties, jwtDecoder, userRepository, tokenProvider, passwordEncoder);
        lenient().when(properties.hasToken()).thenReturn(true);
        lenient().when(properties.getToken()).thenReturn(BOT_TOKEN);
        lenient().when(properties.getClientId()).thenReturn(CLIENT_ID);
        lenient().when(passwordEncoder.encode(any(String.class)))
                .thenAnswer(inv -> "encoded-" + inv.getArgument(0));
    }

    @Test
    void telegramLogin_newUser_createsAndReturnsToken() {
        when(jwtDecoder.decode(anyString())).thenReturn(validIdToken());
        when(userRepository.findByTelegramId(TELEGRAM_ID))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return u;
        });
        when(tokenProvider.generateToken(any(User.class))).thenReturn("tg-jwt");
        when(tokenProvider.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = service.telegramLogin(new TelegramLoginRequest("id-token"));

        assertEquals("tg-jwt", response.token());
        assertEquals(42L, response.userId());
        assertEquals("tg_" + TELEGRAM_ID + "@blossom.local", response.email());
        assertEquals(Role.CUSTOMER, response.role());
    }

    @Test
    void telegramLogin_existingUser_logsIn() {
        when(jwtDecoder.decode(anyString())).thenReturn(validIdToken());
        User existing = new User();
        existing.setId(9L);
        existing.setTelegramId(TELEGRAM_ID);
        existing.setEmail("tg_" + TELEGRAM_ID + "@blossom.local");
        existing.setName("janedoe");
        existing.setIsActive(true);

        when(userRepository.findByTelegramId(TELEGRAM_ID))
                .thenReturn(Optional.of(existing));
        when(tokenProvider.generateToken(existing)).thenReturn("tg-jwt");
        when(tokenProvider.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = service.telegramLogin(new TelegramLoginRequest("id-token"));

        assertEquals(9L, response.userId());
    }

    @Test
    void telegramLogin_deletedUser_rejected() {
        when(jwtDecoder.decode(anyString())).thenReturn(validIdToken());
        User deleted = new User();
        deleted.setId(5L);
        deleted.setTelegramId(TELEGRAM_ID);
        deleted.setIsDeleted(true);

        when(userRepository.findByTelegramId(TELEGRAM_ID))
                .thenReturn(Optional.of(deleted));

        assertThrows(UnauthorizedException.class,
                () -> service.telegramLogin(new TelegramLoginRequest("id-token")));
    }

    @Test
    void telegramLogin_invalidToken_rejected() {
        when(jwtDecoder.decode(anyString()))
                .thenThrow(new JwtException("bad signature"));

        assertThrows(UnauthorizedException.class,
                () -> service.telegramLogin(new TelegramLoginRequest("id-token")));
    }

    @Test
    void telegramLogin_wrongIssuer_rejected() {
        when(jwtDecoder.decode(anyString())).thenReturn(
                idTokenBuilder().claim("iss", "https://example.com").build());

        assertThrows(UnauthorizedException.class,
                () -> service.telegramLogin(new TelegramLoginRequest("id-token")));
    }

    @Test
    void telegramLogin_wrongAudience_rejected() {
        when(jwtDecoder.decode(anyString())).thenReturn(
                idTokenBuilder().audience(List.of("999999")).build());

        assertThrows(UnauthorizedException.class,
                () -> service.telegramLogin(new TelegramLoginRequest("id-token")));
    }

    @Test
    void telegramLogin_disabled_rejected() {
        when(properties.hasToken()).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> service.telegramLogin(new TelegramLoginRequest("id-token")));
    }

    @Test
    void telegramLogin_blankToken_rejected() {
        assertThrows(UnauthorizedException.class,
                () -> service.telegramLogin(new TelegramLoginRequest(" ")));
    }

    private Jwt validIdToken() {
        return idTokenBuilder().build();
    }

    private Jwt.Builder idTokenBuilder() {
        return Jwt.withTokenValue("id-token")
                .header("alg", "RS256")
                .claim("iss", "https://oauth.telegram.org")
                .audience(List.of(CLIENT_ID))
                .claim("id", TELEGRAM_ID)
                .claim("name", "Jane Doe")
                .claim("preferred_username", "janedoe")
                .subject(String.valueOf(TELEGRAM_ID))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));
    }
}