package com.example.spring_boot_project_api.exception;

public class BakongException extends RuntimeException {
    public BakongException(String message) {
        super(message);
    }

    public BakongException(String message, Throwable cause) {
        super(message, cause);
    }
}