package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.customer.CustomerFilterRequest;
import com.example.spring_boot_project_api.dto.request.customer.CustomerUpdateRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.customer.CustomerDetailResponse;
import com.example.spring_boot_project_api.dto.response.customer.CustomerSummaryResponse;

public interface CustomerService {
    PagedResponse<CustomerSummaryResponse> getAllCustomers(CustomerFilterRequest filter);

    CustomerDetailResponse getCustomerById(Long id);

    CustomerSummaryResponse updateCustomer(Long id, CustomerUpdateRequest request);

    void activateCustomer(Long id);

    void deactivateCustomer(Long id);
}
