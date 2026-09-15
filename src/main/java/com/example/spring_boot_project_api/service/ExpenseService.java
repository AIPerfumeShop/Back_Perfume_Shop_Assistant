package com.example.spring_boot_project_api.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.spring_boot_project_api.dto.request.expense.ExpenseRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.expense.ExpenseResponse;

public interface ExpenseService {

    ExpenseResponse createExpense(ExpenseRequest request);

    ExpenseResponse updateExpense(Long id, ExpenseRequest request);

    void deleteExpense(Long id);

    PagedResponse<ExpenseResponse> getExpenses(int page, int size);

    ExpenseResponse getExpense(Long id);

    BigDecimal getTotalExpenses(LocalDate start, LocalDate end);
}