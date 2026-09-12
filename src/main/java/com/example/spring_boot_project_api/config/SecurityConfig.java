package com.example.spring_boot_project_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${app.rate-limit.capacity:5}")
    private int rateLimitCapacity;

    @Value("${app.rate-limit.window-seconds:60}")
    private int rateLimitWindowSeconds;

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
                .requestMatchers("/api/ai/analytics/**").hasRole("ADMIN")
                .requestMatchers("/api/settings/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/payments/*/verify").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/payments/history/order/*").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/payments/history/me").authenticated()
                .requestMatchers("/api/payments/**").hasRole("ADMIN")
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/orders").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/orders/**").hasRole("ADMIN")
                .requestMatchers("/api/cart/**").authenticated()
                .requestMatchers("/api/wishlist/**").authenticated()
                .requestMatchers("/api/reviews/**").authenticated()
                // Public
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/auth/register").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/brands/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/brands/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/brands/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/brands/**").hasRole("ADMIN")
                .requestMatchers("/api/brands/**").permitAll()
                .requestMatchers("/api/categories/**").permitAll()
                .requestMatchers("/api/products/**").permitAll()
                .requestMatchers("/api/upload/**").permitAll()
                .requestMatchers("/api/orders/**").authenticated()
                .requestMatchers("/api/cs/analytics").hasRole("ADMIN")
                .requestMatchers("/api/cs/queue").hasRole("ADMIN")
                .requestMatchers("/api/cs/tickets/*/context").hasRole("ADMIN")
                .requestMatchers("/api/cs/tickets/*/reply").hasRole("ADMIN")
                .requestMatchers("/api/cs/tickets/*/status").hasRole("ADMIN")
                .requestMatchers("/api/cs/tickets/*/priority").hasRole("ADMIN")
                .requestMatchers("/api/cs/tickets/*/notes").hasRole("ADMIN")
                .requestMatchers("/api/cs/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter() {
        RateLimitFilter filter = new RateLimitFilter(rateLimitEnabled, rateLimitCapacity, rateLimitWindowSeconds);
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(-110);
        return registration;
    }

    private void writeJson(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message + "\"}");
    }
}