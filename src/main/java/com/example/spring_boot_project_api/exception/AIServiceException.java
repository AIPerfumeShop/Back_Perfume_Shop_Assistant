package com.example.spring_boot_project_api.exception;

public class AIServiceException extends RuntimeException{
    public AIServiceException(String message){
        super(message);
    }
    public AIServiceException(String message, Throwable cause){
        super(message, cause);
    }
}
