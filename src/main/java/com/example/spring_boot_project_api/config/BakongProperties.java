package com.example.spring_boot_project_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "bakong")
public class BakongProperties {
    private String accountId;
    private String baseUrl;
    private String email;

    public boolean isConfigured() {
        return accountId != null && !accountId.isBlank()
                && baseUrl != null && !baseUrl.isBlank()
                && email != null && !email.isBlank();
    }
}