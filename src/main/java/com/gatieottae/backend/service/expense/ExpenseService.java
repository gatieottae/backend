package com.gatieottae.backend.service.expense;

import com.gatieottae.backend.api.expense.dto.ExpenseRequestDto;
import com.gatieottae.backend.api.expense.dto.ExpenseResponseDto;
import com.gatieottae.backend.common.exception.ConflictException;
import com.gatieottae.backend.domain.expense.Expense;
import com.gatieottae.backend.domain.expense.ExpenseShare;
import com.gatieottae.backend.domain.expense.Transfer;
import com.gatieottae.backend.domain.expense.TransferStatus;
import com.gatieottae.backend.repository.expense.ExpenseRepository;
import com.gatieottae.backend.repository.expense.TransferRepository;
import com.gatieottae.backend.service.notification.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final TransferRepository transferRepository;
    private final NotificationService notificationService;

    public ExpenseResponseDto createExpense(ExpenseRequestDto request) {
        // 1. 분담금 합계 검증
        long totalShares = request.getShares().stream()
                .mapToLong(ExpenseRequestDto.ShareDto::getShareAmount)
                .sum();

        if (totalShares != request.getAmount()) {
            throw new IllegalArgumentException("분담금 합계가 총 지출 금액과 일치하지 않습니다.");
        }

        // 2. 엔티티 생성
        Expense expense = Expense.builder()
                .groupId(request.getGroupId())
                .title(request.getTitle())
                .amount(request.getAmount())
                .paidBy(request.getPaidBy())
                .paidAt(OffsetDateTime.now())
                .build();

        request.getShares().forEach(s ->
                expense.addShare(
                        ExpenseShare.builder()
                                .memberId(s.getMemberId())
                                .shareAmount(s.getShareAmount())
                                .build()
                )
        );

        // 3. 저장
        Expense saved = expenseRepository.save(expense);

        // 그룹 브로드캐스트
        notificationService.notifyExpenseCreated(
                saved.getGroupId(), saved.getId(), saved.getTitle(), saved.getAmount(), saved.getPaidBy()
        );

        return toResponse(saved);
    }

    public ExpenseResponseDto getExpense(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found"));
        return toResponse(expense);
    }

    public List<ExpenseResponseDto> getExpensesByGroup(Long groupId) {
        return expenseRepository.findByGroupIdOrderByPaidAtDesc(groupId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ExpenseResponseDto updateExpense(Long id, ExpenseRequestDto request) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found"));

        expense.setTitle(request.getTitle());
        expense.setAmount(request.getAmount());
        expense.setPaidBy(request.getPaidBy());

        // shares 교체
        expense.getShares().clear();
        request.getShares().forEach(s ->
                expense.addShare(
                        ExpenseShare.builder()
                                .memberId(s.getMemberId())
                                .shareAmount(s.getShareAmount())
                                .build()
                )
        );

        // 그룹 브로드캐스트
        notificationService.notifyExpenseUpdated(
                expense.getGroupId(), expense.getId(), expense.getTitle(), expense.getAmount()
        );

        return toResponse(expense);
    }

    public void deleteExpense(Long groupId, Long expenseId) {
        Expense expense = expenseRepository.findByIdAndGroupId(expenseId, groupId)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found"));

        // 1) 이미 확정된 송금이 하나라도 있으면 삭제 불가
        if (transferRepository.existsByExpenseIdAndStatus(expenseId, TransferStatus.CONFIRMED)) {
            throw new ConflictException("이미 정산에 포함된 지출은 삭제할 수 없습니다.");
        }

        // 2) 확정되지 않은 모든 관련 송금 내역 조회
        List<Transfer> transfersToDelete = transferRepository.findByExpenseIdAndStatusIn(
                expenseId,
                List.of(TransferStatus.REQUESTED, TransferStatus.SENT, TransferStatus.CANCELED, TransferStatus.ROLLED_BACK)
        );

        // 3) 관련 송금 내역 삭제
        if (!transfersToDelete.isEmpty()) {
            transferRepository.deleteAll(transfersToDelete);
        }

        // 그룹 브로드캐스트
        notificationService.notifyExpenseDeleted(groupId, expenseId);

        // 4) 지출 삭제
        expenseRepository.delete(expense);
    }

    private ExpenseResponseDto toResponse(Expense e) {
        return ExpenseResponseDto.builder()
                .id(e.getId())
                .groupId(e.getGroupId())
                .title(e.getTitle())
                .amount(e.getAmount())
                .paidBy(e.getPaidBy())
                .paidAt(e.getPaidAt())
                .shares(
                        e.getShares().stream()
                                .map(s -> ExpenseResponseDto.ShareDto.builder()
                                        .memberId(s.getMemberId())
                                        .shareAmount(s.getShareAmount())
                                        .build())
                                .collect(Collectors.toList())
                )
                .build();
    }
}