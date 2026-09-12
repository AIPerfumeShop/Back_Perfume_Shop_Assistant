package com.example.spring_boot_project_api.util;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.spring_boot_project_api.model.User;

public class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<User> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public static Optional<Long> currentUserId() {
        return currentUser().map(User::getId);
    }
}