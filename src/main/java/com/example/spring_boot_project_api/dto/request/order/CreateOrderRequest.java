package com.example.spring_boot_project_api.dto.request.order;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrderRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Shipping address is required")
    @Size(max = 500, message = "Shipping address must be under 500 characters")
    private String shippingAddress;

    @NotBlank(message = "Phone is required")
    @Size(max = 30, message = "Phone must be under 30 characters")
    private String phone;

    @Valid
    @NotEmpty(message = "Order must contain at least one item")
    private List<OrderItemRequest> items = new ArrayList<>();
}