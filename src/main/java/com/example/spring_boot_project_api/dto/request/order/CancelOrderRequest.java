package com.example.spring_boot_project_api.dto.request.order;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelOrderRequest {
    @Size(max = 500, message = "Cancel reason must be under 500 characters")
    private String reason;
}