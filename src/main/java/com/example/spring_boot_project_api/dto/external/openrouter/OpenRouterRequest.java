package com.example.spring_boot_project_api.dto.external.openrouter;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpenRouterRequest {
    private String model;
    private List<OpenRouterMessage> messages;
    private Boolean stream;
    public OpenRouterRequest(){
    }
    public OpenRouterRequest(
        String model,
        List<OpenRouterMessage> messages
    ){
        this.model = model;
        this.messages = messages;
        this.stream = false;
    }
    public OpenRouterRequest(
        String model,
        List<OpenRouterMessage> messages,
        Boolean stream
    ){
        this.model = model;
        this.messages = messages;
        this.stream = stream;
    }
}
