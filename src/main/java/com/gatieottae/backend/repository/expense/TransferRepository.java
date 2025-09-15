package com.gatieottae.backend.repository.expense;

import com.gatieottae.backend.domain.expense.Transfer;
import com.gatieottae.backend.domain.expense.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
    List<Transfer> findByGroupId(Long groupId);

    List<Transfer> findByGroupIdAndStatus(Long groupId, TransferStatus status);

    Optional<Transfer> findFirstByIdAndGroupId(Long id, Long groupId);

    long countByGroupIdAndFromMemberIdAndToMemberIdAndStatusIn(
            Long groupId, Long fromMemberId, Long toMemberId, List<TransferStatus> statuses);

}