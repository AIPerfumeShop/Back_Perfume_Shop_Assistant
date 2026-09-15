package com.example.spring_boot_project_api.dto.request.payment;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    private String status;

    private String reason;
}
