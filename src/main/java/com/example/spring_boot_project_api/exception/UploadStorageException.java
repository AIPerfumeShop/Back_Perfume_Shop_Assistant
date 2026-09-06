package com.example.spring_boot_project_api.exception;

public class UploadStorageException extends RuntimeException {
    public UploadStorageException(String message) {
        super(message);
    }

    public UploadStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}