package com.gatieottae.backend.repository.expense;

import com.gatieottae.backend.domain.expense.Transfer;
import com.gatieottae.backend.domain.expense.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
    List<Transfer> findByGroupId(Long groupId);
    List<Transfer> findByGroupIdAndStatus(Long groupId, TransferStatus status);
}