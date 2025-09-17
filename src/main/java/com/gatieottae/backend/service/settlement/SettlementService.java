package com.gatieottae.backend.service.settlement;

import com.gatieottae.backend.api.settlement.dto.SettlementResponseDto;
import com.gatieottae.backend.domain.expense.Expense;
import com.gatieottae.backend.domain.expense.ExpenseShare;
import com.gatieottae.backend.domain.expense.Transfer;
import com.gatieottae.backend.repository.expense.ExpenseQueryRepository;
import com.gatieottae.backend.repository.expense.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {

    private final ExpenseQueryRepository expenseQueryRepository;
    private final TransferRepository transferRepository;

    public SettlementResponseDto calculateForMember(Long groupId, Long memberId) {
        // 전체 계산
        SettlementResponseDto overall = calculate(groupId);

        // 내 잔액만 추출
        Long myBalance = overall.getBalances().getOrDefault(memberId, 0L);

        // 내가 관련된 송금 초안만 필터링
        List<SettlementResponseDto.TransferDraft> myDrafts = overall.getTransfersDraft().stream()
                .filter(d -> Objects.equals(d.getFromMemberId(), memberId) || Objects.equals(d.getToMemberId(), memberId))
                .toList();

        return SettlementResponseDto.builder()
                .balances(Map.of(memberId, myBalance)) // 나만의 잔액표
                .transfersDraft(myDrafts)              // 내가 보낼 것/받을 것만
                .build();
    }

    public SettlementResponseDto calculate(Long groupId) {
        // 1. 그룹의 모든 지출 내역 조회
        List<Expense> allExpenses = expenseQueryRepository.findExpensesWithSharesByGroupId(groupId);

        // 2. 그룹의 모든 송금 내역 조회 및 이미 정산된 지출 ID 추출
        List<Transfer> allTransfers = transferRepository.findByGroupId(groupId);
        Set<Long> settledExpenseIds = allTransfers.stream()
                .map(Transfer::getExpenseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 3. 아직 정산되지 않은 지출 내역 필터링
        List<Expense> unsettledExpenses = allExpenses.stream()
                .filter(e -> !settledExpenseIds.contains(e.getId()))
                .toList();

        // 4. 전체 잔액표 계산 (모든 지출을 기반으로 함)
        Map<Long, Long> balances = new HashMap<>();
        for (Expense e : allExpenses) {
            balances.merge(e.getPaidBy(), e.getAmount(), Long::sum);
            for (ExpenseShare s : e.getShares()) {
                balances.merge(s.getMemberId(), -s.getShareAmount(), Long::sum);
            }
        }

        // 5. 정산되지 않은 지출에 대해서만 송금 초안 생성
        List<SettlementResponseDto.TransferDraft> drafts = new ArrayList<>();
        for (Expense expense : unsettledExpenses) {
            Long payer = expense.getPaidBy();
            for (ExpenseShare share : expense.getShares()) {
                if (Objects.equals(share.getMemberId(), payer)) {
                    continue;
                }
                if (share.getShareAmount() > 0) {
                    drafts.add(SettlementResponseDto.TransferDraft.builder()
                            .fromMemberId(share.getMemberId())
                            .toMemberId(payer)
                            .amount(share.getShareAmount())
                            .expenseId(expense.getId()) // 지출 ID 연결
                            .build());
                }
            }
        }

        // 6. 응답
        return SettlementResponseDto.builder()
                .balances(balances)
                .transfersDraft(drafts)
                .build();
    }
}