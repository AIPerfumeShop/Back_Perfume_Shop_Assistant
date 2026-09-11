package com.example.spring_boot_project_api.service.impl;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.config.GoogleAuthProperties;
import com.example.spring_boot_project_api.config.JwtTokenProvider;
import com.example.spring_boot_project_api.dto.request.auth.GoogleLoginRequest;
import com.example.spring_boot_project_api.dto.response.auth.AuthResponse;
import com.example.spring_boot_project_api.enums.Role;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.UnauthorizedException;
import com.example.spring_boot_project_api.mapper.UserMapper;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.GoogleAuthService;

@Service
public class GoogleAuthServiceImpl implements GoogleAuthService {

    private static final String GOOGLE_ISSUER = "https://accounts.google.com";
    private static final String GOOGLE_ISSUER_ALT = "accounts.google.com";

    private final GoogleAuthProperties properties;
    private final JwtDecoder jwtDecoder;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    public GoogleAuthServiceImpl(GoogleAuthProperties properties,
                                 @Qualifier("googleJwtDecoder") JwtDecoder jwtDecoder,
                                 UserRepository userRepository,
                                 JwtTokenProvider tokenProvider,
                                 PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.jwtDecoder = jwtDecoder;
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        if (!properties.isEnabled()) {
            throw new BadRequestException(
                    "Google sign-in is not configured on this server");
        }

        Jwt idToken = validateIdToken(request.idToken());

        String email = idToken.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new UnauthorizedException(
                    "Google account has no email address");
        }
        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> createUser(idToken, normalizedEmail));

        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new UnauthorizedException("Account has been deleted");
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            user.setIsActive(true);
            userRepository.save(user);
        }

        String token = tokenProvider.generateToken(user);
        return AuthResponse.of(
                token, tokenProvider.getExpirationMs(),
                UserMapper.toUserResponse(user));
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    public String clientId() {
        return properties.getClientId();
    }

    private Jwt validateIdToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new UnauthorizedException("Google ID token is required");
        }

        final Jwt jwt;
        try {
            jwt = jwtDecoder.decode(idToken);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException(
                    "Invalid or expired Google ID token");
        }

        String issuer = jwt.getClaimAsString("iss");
        if (!GOOGLE_ISSUER.equals(issuer)
                && !GOOGLE_ISSUER_ALT.equals(issuer)) {
            throw new UnauthorizedException("Invalid Google ID token issuer");
        }

        List<String> audiences = jwt.getAudience();
        if (audiences == null
                || audiences.stream()
                        .noneMatch(properties.getClientId()::equals)) {
            throw new UnauthorizedException(
                    "Google ID token is not intended for this application");
        }

        return jwt;
    }

    private User createUser(Jwt idToken, String email) {
        User user = new User();
        user.setName(displayName(idToken.getClaimAsString("name")));
        user.setEmail(email);
        user.setPassword(autogeneratedPassword());
        user.setRole(Role.CUSTOMER);
        user.setIsActive(true);
        return userRepository.save(user);
    }

    private String displayName(String name) {
        if (name == null || name.isBlank()) {
            return "Google User";
        }
        return name.trim();
    }

    private String autogeneratedPassword() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return passwordEncoder.encode(
                Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(bytes));
    }
}