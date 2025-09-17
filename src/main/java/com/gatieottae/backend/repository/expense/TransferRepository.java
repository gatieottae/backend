package com.gatieottae.backend.repository.expense;

import com.gatieottae.backend.domain.expense.Expense;
import com.gatieottae.backend.domain.expense.Transfer;
import com.gatieottae.backend.domain.expense.TransferStatus;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    Optional<Transfer> findFirstByIdAndGroupId(Long id, Long groupId);

    /**
     * 특정 멤버 기준(보내는 사람 or 받는 사람)으로 해당 그룹의 송금 목록 조회
     */
    @Query("""
      select t from Transfer t
      where t.groupId = :groupId
        and (t.fromMemberId = :memberId or t.toMemberId = :memberId)
      order by t.createdAt desc
    """)
    List<Transfer> findMyTransfersInGroup(Long groupId, Long memberId);

    List<Transfer> findByGroupId(Long groupId);
 

    boolean existsByExpenseIdAndStatus(Long expenseId, TransferStatus transferStatus);

    List<Transfer> findByExpenseIdAndStatusIn(Long expenseId, List<TransferStatus> requested);

    long countByGroupIdAndFromMemberIdAndToMemberIdAndExpenseIdAndStatusIn(
            Long groupId, Long fromMemberId, Long toMemberId, Long expenseId, Collection<TransferStatus> statuses);

    Optional<Transfer> findFirstByGroupIdAndFromMemberIdAndToMemberIdAndExpenseIdAndStatusInOrderByCreatedAtDesc(
            Long groupId, Long fromMemberId, Long toMemberId, Long expenseId, Collection<TransferStatus> statuses);

    Optional<Transfer> findFirstByExpenseIdAndFromMemberIdAndToMemberId(Long expenseId, Long fromMemberId, Long toMemberId);
}