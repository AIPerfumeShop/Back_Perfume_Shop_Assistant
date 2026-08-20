package com.example.spring_boot_project_api.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration  //used to indicate that the class is a configuration class in Spring Boot
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigSource(){
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of(
            "GET","POST","PUT","DELETE","PATCH","OPTIONS"
        ));
        configuration.setAllowedHeaders(List.of(
            "Authorization","Content-Type","Accept"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
