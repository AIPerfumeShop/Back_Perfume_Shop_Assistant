package com.example.spring_boot_project_api.dto.request.order;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutRequest extends CreateOrderRequest {

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    //City/province of the delivery address. Used to gate payment methods
    //that are only offered in certain locations (e.g. CASH in Phnom Penh).
    private String city;

}
