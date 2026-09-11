package com.example.spring_boot_project_api.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class TelegramAuthServiceImplTest {

    private static final String BOT_TOKEN = "123456:TEST-BOT-TOKEN";
    private static final String BOT_USERNAME = "BlossomPerfumeBot";
    private static final long TELEGRAM_ID = 279058397L;

    @Mock
    private TelegramBotProperties properties;
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
                properties, userRepository, tokenProvider, passwordEncoder);
        lenient().when(properties.hasToken()).thenReturn(true);
        lenient().when(properties.getToken()).thenReturn(BOT_TOKEN);
        lenient().when(properties.getUsername()).thenReturn(BOT_USERNAME);
        lenient().when(passwordEncoder.encode(any(String.class)))
                .thenAnswer(inv -> "encoded-" + inv.getArgument(0));
    }

    @Test
    void telegramLogin_newUser_createsAndReturnsToken() {
        TelegramLoginRequest request = signedRequest();
        when(userRepository.findByTelegramId(TELEGRAM_ID))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return u;
        });
        when(tokenProvider.generateToken(any(User.class)))
                .thenReturn("tg-jwt");
        when(tokenProvider.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = service.telegramLogin(request);

        assertEquals("tg-jwt", response.token());
        assertEquals(42L, response.userId());
        assertEquals("tg_" + TELEGRAM_ID + "@blossom.local", response.email());
        assertEquals(Role.CUSTOMER, response.role());
    }

    @Test
    void telegramLogin_existingUser_logsIn() {
        long authDate = System.currentTimeMillis() / 1000L - 60;
        TelegramLoginRequest request = new TelegramLoginRequest(
                TELEGRAM_ID, "", null, null, null, authDate,
                hmacHex("auth_date=" + authDate + "\nuser_id=" + TELEGRAM_ID,
                        BOT_TOKEN));
        User existing = new User();
        existing.setId(9L);
        existing.setTelegramId(TELEGRAM_ID);
        existing.setEmail("tg_" + TELEGRAM_ID + "@blossom.local");
        existing.setIsActive(true);

        when(userRepository.findByTelegramId(TELEGRAM_ID))
                .thenReturn(Optional.of(existing));
        when(tokenProvider.generateToken(existing)).thenReturn("tg-jwt");
        when(tokenProvider.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = service.telegramLogin(request);

        assertEquals(9L, response.userId());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void telegramLogin_deletedUser_rejected() {
        TelegramLoginRequest request = signedRequest();
        User deleted = new User();
        deleted.setId(5L);
        deleted.setTelegramId(TELEGRAM_ID);
        deleted.setIsDeleted(true);

        when(userRepository.findByTelegramId(TELEGRAM_ID))
                .thenReturn(Optional.of(deleted));

        assertThrows(UnauthorizedException.class,
                () -> service.telegramLogin(request));
    }

    @Test
    void telegramLogin_invalidHash_rejected() {
        TelegramLoginRequest valid = signedRequest();
        TelegramLoginRequest request = new TelegramLoginRequest(
                valid.id(), valid.firstName(), valid.lastName(),
                valid.username(), valid.photoUrl(), valid.authDate(),
                "0000000000000000000000000000000000000000000000000000000000000000");

        assertThrows(UnauthorizedException.class,
                () -> service.telegramLogin(request));
    }

    @Test
    void telegramLogin_expiredAuthDate_rejected() {
        long oldDate = System.currentTimeMillis() / 1000L - 25 * 60 * 60L;
        TelegramLoginRequest request = new TelegramLoginRequest(
                TELEGRAM_ID, "Jane", null, null, null, oldDate,
                hmacHex("auth_date=" + oldDate + "\nuser_id=" + TELEGRAM_ID, BOT_TOKEN));

        assertThrows(UnauthorizedException.class,
                () -> service.telegramLogin(request));
    }

    @Test
    void telegramLogin_disabled_rejected() {
        when(properties.hasToken()).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> service.telegramLogin(signedRequest()));
    }

    @Test
    void telegramLogin_missingFields_rejected() {
        TelegramLoginRequest request = new TelegramLoginRequest(
                TELEGRAM_ID, "Jane", null, null, null,
                System.currentTimeMillis() / 1000L, null);

        assertThrows(UnauthorizedException.class,
                () -> service.telegramLogin(request));
    }

    private TelegramLoginRequest signedRequest() {
        long authDate = System.currentTimeMillis() / 1000L - 60;
        String data = "auth_date=" + authDate + "\nuser_id=" + TELEGRAM_ID;
        return new TelegramLoginRequest(
                TELEGRAM_ID, "Jane", "Doe", "janedoe", null, authDate,
                hmacHex(data, BOT_TOKEN));
    }

    private String hmacHex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(
                    mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}