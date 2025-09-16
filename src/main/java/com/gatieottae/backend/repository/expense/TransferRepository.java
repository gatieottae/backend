package com.gatieottae.backend.repository.expense;

import com.gatieottae.backend.domain.expense.Expense;
import com.gatieottae.backend.domain.expense.Transfer;
import com.gatieottae.backend.domain.expense.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    /**
     * 동일 (groupId, fromMemberId, toMemberId) 조합에 대해
     * 특정 상태 집합(statuses)에 포함되는 건수를 센다.
     * - 멱등 커밋 중복 판단 용도
     */
    long countByGroupIdAndFromMemberIdAndToMemberIdAndStatusIn(
            Long groupId, Long fromMemberId, Long toMemberId, Collection<TransferStatus> statuses);

    /**
     * id + groupId 로 단건 조회 (정확히 한 건)
     */
    Optional<Transfer> findFirstByIdAndGroupId(Long id, Long groupId);

    /**
     * 동일 (groupId, fromMemberId, toMemberId) 조합에서
     * 진행중 상태 집합(statuses) 중 "가장 최근 1건"을 찾는다.
     * - 멱등 커밋 시 기존 건 반환 용도
     */
    Optional<Transfer> findFirstByGroupIdAndFromMemberIdAndToMemberIdAndStatusInOrderByCreatedAtDesc(
            Long groupId, Long fromMemberId, Long toMemberId, Collection<TransferStatus> statuses);

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
 

    boolean existsByExpenseIdAndStatus(Long expenseId, TransferStatus transferStatus);

    List<Transfer> findByExpenseIdAndStatusIn(Long expenseId, List<TransferStatus> requested);
}