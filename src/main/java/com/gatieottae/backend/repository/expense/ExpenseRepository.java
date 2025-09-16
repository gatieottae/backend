package com.gatieottae.backend.repository.expense;

import com.gatieottae.backend.domain.expense.Expense;
import com.gatieottae.backend.domain.expense.Transfer;
import com.gatieottae.backend.domain.expense.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByGroupIdOrderByPaidAtDesc(Long groupId);

    Optional<Expense> findByIdAndGroupId(Long id, Long groupId);
}