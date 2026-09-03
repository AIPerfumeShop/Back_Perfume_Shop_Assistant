package com.example.spring_boot_project_api.dto.request.order;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutRequest extends CreateOrderRequest {

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;
    
}
