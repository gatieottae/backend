package com.gatieottae.backend.repository.expense;

import com.gatieottae.backend.domain.expense.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByGroupIdOrderByPaidAtDesc(Long groupId);
}