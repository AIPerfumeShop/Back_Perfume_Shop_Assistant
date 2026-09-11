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
    private String clientId;

    public boolean hasToken() {
        return token != null && !token.isBlank();
    }

    public boolean hasChatId() {
        return chatId != null && !chatId.isBlank();
    }

    public boolean isEnabled() {
        return enabled && hasToken() && hasChatId();
    }

    /**
     * Client ID for the OIDC Telegram Login library. Prefer an explicit
     * {@code telegram.bot.client-id} override; otherwise the numeric prefix of
     * the bot token (the bot's own user id) is used.
     */
    public String getClientId() {
        if (clientId != null && !clientId.isBlank()) {
            return clientId;
        }
        int colon = token == null ? -1 : token.indexOf(':');
        return colon > 0 ? token.substring(0, colon) : null;
    }
}