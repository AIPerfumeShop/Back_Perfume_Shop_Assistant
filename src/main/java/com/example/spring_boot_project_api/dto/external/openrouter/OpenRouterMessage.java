package com.example.spring_boot_project_api.dto.external.openrouter;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class OpenRouterMessage {
    private String role;
    private String content;
    public OpenRouterMessage() {
    }
    public OpenRouterMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
}
