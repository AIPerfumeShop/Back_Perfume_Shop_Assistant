package com.example.spring_boot_project_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class TelegramAuthConfig {

    @Bean("telegramJwtDecoder")
    public JwtDecoder telegramJwtDecoder() {
        return NimbusJwtDecoder
                .withIssuerLocation("https://oauth.telegram.org")
                .build();
    }
}