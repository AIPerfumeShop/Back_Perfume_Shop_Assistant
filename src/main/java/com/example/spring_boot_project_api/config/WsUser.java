package com.example.spring_boot_project_api.config;

import java.security.Principal;

// Authenticated WebSocket user resolved from the JWT in the STOMP CONNECT
// frame. Admins act as customer-care agents; everyone else is a customer.
public record WsUser(Long id, String role) implements Principal {

    @Override
    public String getName() {
        return String.valueOf(id);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}