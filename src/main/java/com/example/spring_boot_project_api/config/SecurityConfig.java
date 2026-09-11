package com.example.spring_boot_project_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigSource) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigSource))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, ex) -> writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                .accessDeniedHandler((request, response, ex) -> writeJson(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden"))
            )
            .authorizeHttpRequests(auth -> auth
                // Admin panel (JWT role gated)
                .requestMatchers("/api/auth/me").authenticated()
                .requestMatchers("/api/auth/change-password").authenticated()
                .requestMatchers("/api/auth/change-email/**").authenticated()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/dashboard/**").hasRole("ADMIN")
                .requestMatchers("/api/analytics/**").hasRole("ADMIN")
                .requestMatchers("/api/statistics/**").hasRole("ADMIN")
                .requestMatchers("/api/customers/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/payments/*/verify").authenticated()
                .requestMatchers("/api/payments/**").hasRole("ADMIN")
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                // Public
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/auth/register").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/ai/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/api/brands/**").permitAll()
                .requestMatchers("/api/categories/**").permitAll()
                .requestMatchers("/api/products/**").permitAll()
                .requestMatchers("/api/upload/**").permitAll()
                .requestMatchers("/api/orders/**").authenticated()
                .requestMatchers("/api/cs/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void writeJson(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message + "\"}");
    }
}