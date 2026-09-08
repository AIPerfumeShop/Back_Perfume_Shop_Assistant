package com.example.spring_boot_project_api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.customer.CustomerFilterRequest;
import com.example.spring_boot_project_api.dto.request.customer.CustomerUpdateRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.customer.CustomerDetailResponse;
import com.example.spring_boot_project_api.dto.response.customer.CustomerSummaryResponse;
import com.example.spring_boot_project_api.service.CustomerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/customers")
@Validated
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;

    @Operation(summary = "Get all customers with search, role/active filters and pagination")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customers retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<PagedResponse<CustomerSummaryResponse>> getAllCustomers(
            @ModelAttribute CustomerFilterRequest filter) {
        PagedResponse<CustomerSummaryResponse> response = customerService.getAllCustomers(filter);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get customer details with order history")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDetailResponse> getCustomerById(
            @PathVariable Long id) {
        CustomerDetailResponse response = customerService.getCustomerById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update customer information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer updated successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input or duplicate email")
    })
    @PutMapping("/{id}")
    public ResponseEntity<CustomerSummaryResponse> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerUpdateRequest request) {
        CustomerSummaryResponse response = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Activate a customer account")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Customer activated successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "400", description = "Customer is already active")
    })
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateCustomer(@PathVariable Long id) {
        customerService.activateCustomer(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Deactivate a customer account")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Customer deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "400", description = "Customer is already deactivated")
    })
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateCustomer(@PathVariable Long id) {
        customerService.deactivateCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
