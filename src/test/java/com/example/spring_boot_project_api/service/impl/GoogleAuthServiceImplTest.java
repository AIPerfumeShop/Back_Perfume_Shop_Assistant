package com.example.spring_boot_project_api.service.impl;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.spring_boot_project_api.config.GoogleAuthProperties;
import com.example.spring_boot_project_api.config.JwtTokenProvider;
import com.example.spring_boot_project_api.dto.request.auth.GoogleLoginRequest;
import com.example.spring_boot_project_api.dto.response.auth.AuthResponse;
import com.example.spring_boot_project_api.enums.Role;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.UnauthorizedException;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceImplTest {

    @Mock
    private GoogleAuthProperties properties;
    @Mock
    private JwtDecoder jwtDecoder;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private PasswordEncoder passwordEncoder;

    private GoogleAuthServiceImpl service;

    private static final String CLIENT_ID =
            "1234567890-abc.apps.googleusercontent.com";

    @BeforeEach
    void setUp() {
        service = new GoogleAuthServiceImpl(
                properties, jwtDecoder, userRepository, tokenProvider,
                passwordEncoder);
        lenient().when(properties.getClientId()).thenReturn(CLIENT_ID);
        lenient().when(properties.isEnabled()).thenReturn(true);
    }

    @Test
    void googleLogin_newUser_registersAndReturnsToken() {
        Jwt idToken = googleIdToken("jane@example.com", "Jane Doe");
        when(jwtDecoder.decode("idtoken")).thenReturn(idToken);
        when(userRepository.findByEmail("jane@example.com"))
                .thenReturn(Optional.empty());

        User saved = new User();
        saved.setId(7L);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(7L);
            return u;
        });
        when(tokenProvider.generateToken(any(User.class)))
                .thenReturn("jwt-token");
        when(tokenProvider.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = service.googleLogin(
                new GoogleLoginRequest("idtoken"));

        assertEquals("jwt-token", response.token());
        assertEquals(7L, response.userId());
        assertEquals("jane@example.com", response.email());
        assertEquals(Role.CUSTOMER, response.role());
        assertTrue(response.name().contains("Jane"));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void googleLogin_existingActiveUser_logsIn() {
        Jwt idToken = googleIdToken("jane@example.com", "Jane Doe");
        User existing = new User();
        existing.setId(3L);
        existing.setName("Jane");
        existing.setEmail("jane@example.com");
        existing.setRole(Role.CUSTOMER);
        existing.setIsActive(true);

        when(jwtDecoder.decode("idtoken")).thenReturn(idToken);
        when(userRepository.findByEmail("jane@example.com"))
                .thenReturn(Optional.of(existing));
        when(tokenProvider.generateToken(existing)).thenReturn("jwt-token");
        when(tokenProvider.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = service.googleLogin(
                new GoogleLoginRequest("idtoken"));

        assertEquals(3L, response.userId());
        assertEquals("jane@example.com", response.email());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void googleLogin_existingInactiveUser_activates() {
        Jwt idToken = googleIdToken("bob@example.com", "Bob");
        User existing = new User();
        existing.setId(9L);
        existing.setEmail("bob@example.com");
        existing.setIsActive(false);

        when(jwtDecoder.decode("idtoken")).thenReturn(idToken);
        when(userRepository.findByEmail("bob@example.com"))
                .thenReturn(Optional.of(existing));
        when(tokenProvider.generateToken(existing)).thenReturn("jwt-token");
        when(tokenProvider.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = service.googleLogin(
                new GoogleLoginRequest("idtoken"));

        assertEquals(9L, response.userId());
        assertTrue(existing.getIsActive());
        verify(userRepository).save(existing);
    }

    @Test
    void googleLogin_deletedUser_rejected() {
        Jwt idToken = googleIdToken("dead@example.com", "Dead");
        User deleted = new User();
        deleted.setId(5L);
        deleted.setEmail("dead@example.com");
        deleted.setIsActive(true);
        deleted.setIsDeleted(true);

        when(jwtDecoder.decode("idtoken")).thenReturn(idToken);
        when(userRepository.findByEmail("dead@example.com"))
                .thenReturn(Optional.of(deleted));

        assertThrows(UnauthorizedException.class,
                () -> service.googleLogin(new GoogleLoginRequest("idtoken")));
    }

    @Test
    void googleLogin_invalidSignature_rejected() {
        when(jwtDecoder.decode("bad-token"))
                .thenThrow(new org.springframework.security.oauth2.jwt.JwtException("bad sig"));

        assertThrows(UnauthorizedException.class,
                () -> service.googleLogin(new GoogleLoginRequest("bad-token")));
    }

    @Test
    void googleLogin_wrongIssuer_rejected() {
        Jwt idToken = Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .issuer("https://evil.example.com")
                .subject("sub")
                .audience(List.of(CLIENT_ID))
                .claim("email", "hacker@example.com")
                .claim("name", "Hacker")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(jwtDecoder.decode("idtoken")).thenReturn(idToken);

        assertThrows(UnauthorizedException.class,
                () -> service.googleLogin(new GoogleLoginRequest("idtoken")));
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void googleLogin_wrongAudience_rejected() {
        Jwt idToken = Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .issuer("https://accounts.google.com")
                .subject("sub")
                .audience(List.of("other-client.apps.googleusercontent.com"))
                .claim("email", "hacker@example.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(jwtDecoder.decode("idtoken")).thenReturn(idToken);

        assertThrows(UnauthorizedException.class,
                () -> service.googleLogin(new GoogleLoginRequest("idtoken")));
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void googleLogin_disabled_rejected() {
        when(properties.isEnabled()).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> service.googleLogin(new GoogleLoginRequest("anything")));
        verify(jwtDecoder, never()).decode(any());
    }

    @Test
    void googleLogin_missingToken_rejected() {
        assertThrows(UnauthorizedException.class,
                () -> service.googleLogin(new GoogleLoginRequest("  ")));
    }

    private Jwt googleIdToken(String email, String name) {
        return Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .issuer("https://accounts.google.com")
                .subject("google-sub")
                .audience(List.of(CLIENT_ID))
                .claim("email", email)
                .claim("name", name)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}