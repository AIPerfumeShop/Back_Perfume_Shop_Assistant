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

import com.example.spring_boot_project_api.config.JwtTokenProvider;
import com.example.spring_boot_project_api.config.TelegramBotProperties;
import com.example.spring_boot_project_api.dto.request.auth.TelegramLoginRequest;
import com.example.spring_boot_project_api.dto.response.auth.AuthResponse;
import com.example.spring_boot_project_api.enums.Role;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.UnauthorizedException;
import com.example.spring_boot_project_api.mapper.UserMapper;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.TelegramAuthService;

@Service
public class TelegramAuthServiceImpl implements TelegramAuthService {

    private static final String TELEGRAM_ISSUER = "https://oauth.telegram.org";
    private static final String EMAIL_DOMAIN = "blossom.local";
    private static final String SYNT_EMAIL_PREFIX = "tg_";

    private final TelegramBotProperties properties;
    private final JwtDecoder jwtDecoder;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    public TelegramAuthServiceImpl(TelegramBotProperties properties,
                                   @Qualifier("telegramJwtDecoder") JwtDecoder jwtDecoder,
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
    public AuthResponse telegramLogin(TelegramLoginRequest request) {
        if (!isEnabled()) {
            throw new BadRequestException(
                    "Telegram sign-in is not configured on this server");
        }

        Jwt idToken = validateIdToken(request.idToken());
        Long telegramId = telegramIdOf(idToken);

        User user = userRepository.findByTelegramId(telegramId)
                .map(existing -> {
                    if (Boolean.TRUE.equals(existing.getIsDeleted())) {
                        throw new UnauthorizedException("Account has been deleted");
                    }
                    updateTelegramProfile(existing, idToken);
                    return existing;
                })
                .orElseGet(() -> createUser(idToken, telegramId));

        String token = tokenProvider.generateToken(user);
        return AuthResponse.of(
                token, tokenProvider.getExpirationMs(),
                UserMapper.toUserResponse(user));
    }

    @Override
    public boolean isEnabled() {
        return properties.hasToken() && properties.getClientId() != null;
    }

    @Override
    public String clientId() {
        return properties.getClientId();
    }

    private Jwt validateIdToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new UnauthorizedException("Telegram ID token is required");
        }

        final Jwt jwt;
        try {
            jwt = jwtDecoder.decode(idToken);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException(
                    "Invalid or expired Telegram ID token");
        }

        String issuer = jwt.getClaimAsString("iss");
        if (!TELEGRAM_ISSUER.equals(issuer)) {
            throw new UnauthorizedException("Invalid Telegram ID token issuer");
        }

        String clientId = properties.getClientId();
        List<String> audiences = jwt.getAudience();
        if (clientId == null || audiences == null
                || audiences.stream().noneMatch(clientId::equals)) {
            throw new UnauthorizedException(
                    "Telegram ID token is not intended for this application");
        }

        return jwt;
    }

    private Long telegramIdOf(Jwt jwt) {
        Number id = jwt.getClaim("id");
        if (id != null) {
            return id.longValue();
        }
        String sub = jwt.getSubject();
        if (sub != null && sub.matches("\\d+")) {
            return Long.parseLong(sub);
        }
        throw new UnauthorizedException("Telegram ID token is missing user id");
    }

    private User createUser(Jwt idToken, Long telegramId) {
        User user = new User();
        user.setName(displayName(idToken));
        user.setEmail(syntheticEmail(telegramId));
        user.setPassword(autogeneratedPassword());
        user.setRole(Role.CUSTOMER);
        user.setIsActive(true);
        user.setTelegramId(telegramId);
        user.setTelegramPhotoUrl(photoUrl(idToken));
        return userRepository.save(user);
    }

    private void updateTelegramProfile(User user, Jwt idToken) {
        boolean changed = false;
        String name = displayName(idToken);
        if (name != null && !name.equals(user.getName())) {
            user.setName(name);
            changed = true;
        }
        String photoUrl = photoUrl(idToken);
        if (photoUrl != null && !photoUrl.equals(user.getTelegramPhotoUrl())) {
            user.setTelegramPhotoUrl(photoUrl);
            changed = true;
        }
        if (changed) {
            userRepository.save(user);
        }
    }

    private String displayName(Jwt idToken) {
        String username = idToken.getClaimAsString("preferred_username");
        if (username != null && !username.isBlank()) {
            return username.trim();
        }
        String name = idToken.getClaimAsString("name");
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        return "Telegram User";
    }

    private String photoUrl(Jwt idToken) {
        String picture = idToken.getClaimAsString("picture");
        return picture == null || picture.isBlank() ? null : picture;
    }

    private String syntheticEmail(Long telegramId) {
        return SYNT_EMAIL_PREFIX + telegramId + "@" + EMAIL_DOMAIN;
    }

    private String autogeneratedPassword() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return passwordEncoder.encode(
                Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(bytes));
    }
}