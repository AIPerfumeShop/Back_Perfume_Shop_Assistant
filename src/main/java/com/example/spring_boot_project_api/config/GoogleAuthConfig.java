package com.example.spring_boot_project_api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
@EnableConfigurationProperties(GoogleAuthProperties.class)
public class GoogleAuthConfig {

    @Bean("googleJwtDecoder")
    public JwtDecoder googleJwtDecoder() {
        return NimbusJwtDecoder
                .withIssuerLocation("https://accounts.google.com")
                .build();
    }
}