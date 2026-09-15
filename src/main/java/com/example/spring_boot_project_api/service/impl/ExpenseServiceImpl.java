package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.expense.ExpenseRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.expense.ExpenseResponse;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.model.Expense;
import com.example.spring_boot_project_api.repository.ExpenseRepository;
import com.example.spring_boot_project_api.service.ExpenseService;

@Service
@Transactional
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;

    public ExpenseServiceImpl(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Override
    public ExpenseResponse createExpense(ExpenseRequest request) {
        Expense expense = new Expense();
        applyRequest(expense, request);
        return toResponse(expenseRepository.save(expense));
    }

    @Override
    public ExpenseResponse updateExpense(Long id, ExpenseRequest request) {
        Expense expense = findExpense(id);
        applyRequest(expense, request);
        return toResponse(expenseRepository.save(expense));
    }

    @Override
    public void deleteExpense(Long id) {
        expenseRepository.delete(findExpense(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ExpenseResponse> getExpenses(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<Expense> expenses = expenseRepository.findAllByOrderByIncurredAtDescIdDesc(
                PageRequest.of(safePage, safeSize,
                        Sort.by(Sort.Order.desc("incurredAt"), Sort.Order.desc("id"))));
        List<ExpenseResponse> content = expenses.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new PagedResponse<>(
                content, expenses.getTotalElements(),
                expenses.getTotalPages(), expenses.getNumber(), expenses.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseResponse getExpense(Long id) {
        return toResponse(findExpense(id));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalExpenses(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return expenseRepository.sumAmountBetween(LocalDate.MIN, LocalDate.MAX);
        }
        return expenseRepository.sumAmountBetween(start, end);
    }

    private void applyRequest(Expense expense, ExpenseRequest request) {
        expense.setTitle(request.getTitle().trim());
        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setNote(request.getNote());
        expense.setIncurredAt(request.getIncurredAt());
    }

    private Expense findExpense(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Expense not found with ID : " + id));
    }

    private ExpenseResponse toResponse(Expense expense) {
        ExpenseResponse response = new ExpenseResponse();
        response.setId(expense.getId());
        response.setTitle(expense.getTitle());
        response.setAmount(expense.getAmount());
        response.setCategory(expense.getCategory());
        response.setNote(expense.getNote());
        response.setIncurredAt(expense.getIncurredAt());
        response.setCreatedAt(expense.getCreatedAt());
        return response;
    }
}