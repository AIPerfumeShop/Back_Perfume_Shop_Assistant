package com.example.spring_boot_project_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "google.auth")
public class GoogleAuthProperties {
    private String clientId;

    public boolean isEnabled() {
        return clientId != null && !clientId.isBlank();
    }
}