package com.example.spring_boot_project_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customerOpenAPI(){
        return new OpenAPI()
        .info(new Info()
                .title("Blossom Fragrance API")
                .version("1.0.0")
                .description("API for Blossom Fragrance Perfume Shop")
    );
    }
}
