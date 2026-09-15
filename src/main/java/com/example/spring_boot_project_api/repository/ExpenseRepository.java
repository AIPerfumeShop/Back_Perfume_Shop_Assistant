package com.example.spring_boot_project_api.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.Expense;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Page<Expense> findAllByOrderByIncurredAtDescIdDesc(Pageable pageable);

    @Query("select coalesce(sum(e.amount), 0) from Expense e " +
            "where e.incurredAt >= :start and e.incurredAt <= :end")
    BigDecimal sumAmountBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    List<Expense> findTop5ByOrderByIncurredAtDescIdDesc();
}