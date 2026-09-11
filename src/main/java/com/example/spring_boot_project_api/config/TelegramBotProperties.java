package com.example.spring_boot_project_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "telegram.bot")
public class TelegramBotProperties {
    private String token;
    private String username;
    private String chatId;
    private boolean enabled = false;

    public boolean hasToken() {
        return token != null && !token.isBlank();
    }

    public boolean hasChatId() {
        return chatId != null && !chatId.isBlank();
    }

    public boolean isEnabled() {
        return enabled && hasToken() && hasChatId();
    }
}