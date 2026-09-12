package com.example.spring_boot_project_api.config;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RateLimitFilter extends OncePerRequestFilter {

    private final boolean enabled;
    private final double capacity;
    private final double refillPerSecond;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private static final Set<String> SENSITIVE_ENDPOINTS = Set.of(
            "POST /api/auth/login",
            "POST /api/auth/register",
            "POST /api/auth/verify-email",
            "POST /api/auth/resend-otp",
            "POST /api/auth/forgot-password",
            "POST /api/auth/verify-otp",
            "POST /api/auth/reset-password");

    public RateLimitFilter(boolean enabled, int capacity, int windowSeconds) {
        this.enabled = enabled;
        this.capacity = capacity;
        this.refillPerSecond = windowSeconds > 0 ? (double) capacity / windowSeconds : Double.MAX_VALUE;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (enabled && isSensitive(request.getMethod(), request.getRequestURI())) {
            String key = clientKey(request) + "|" + request.getMethod() + "|" + request.getRequestURI();
            Bucket bucket = buckets.computeIfAbsent(key,
                    ignored -> new Bucket(capacity, refillPerSecond));
            if (!bucket.tryAcquire()) {
                writeTooManyRequests(response);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isSensitive(String method, String uri) {
        return SENSITIVE_ENDPOINTS.contains(method + " " + uri);
    }

    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf((long) Math.ceil(capacity / refillPerSecond)));
        response.getWriter().write(
                "{\"status\":429,\"message\":\"Too many requests. Please slow down and try again later.\","
                + "\"timestamp\":\"" + LocalDateTime.now() + "\"}");
    }

    private static final class Bucket {
        private final double capacity;
        private final double refillPerSecond;
        private double tokens;
        private long lastRefillNanos;

        Bucket(double capacity, double refillPerSecond) {
            this.capacity = capacity;
            this.refillPerSecond = refillPerSecond;
            this.tokens = capacity;
            this.lastRefillNanos = System.nanoTime();
        }

        synchronized boolean tryAcquire() {
            long now = System.nanoTime();
            double elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0;
            tokens = Math.min(capacity, tokens + elapsedSeconds * refillPerSecond);
            lastRefillNanos = now;
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }
    }
}