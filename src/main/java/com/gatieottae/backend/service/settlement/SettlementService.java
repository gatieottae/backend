package com.gatieottae.backend.service.settlement;

import com.gatieottae.backend.api.settlement.dto.SettlementResponseDto;
import com.gatieottae.backend.domain.expense.Expense;
import com.gatieottae.backend.domain.expense.ExpenseShare;
import com.gatieottae.backend.repository.expense.ExpenseQueryRepository;
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
        List<Expense> expenses = expenseQueryRepository.findExpensesWithSharesByGroupId(groupId);

        // 1. 최종 잔액표 계산 (기존과 동일)
        Map<Long, Long> balances = new HashMap<>();
        for (Expense e : expenses) {
            // 지불자 +
            balances.merge(e.getPaidBy(), e.getAmount(), Long::sum);
            // 분담자 -
            for (ExpenseShare s : e.getShares()) {
                balances.merge(s.getMemberId(), -s.getShareAmount(), Long::sum);
            }
        }

        // 2. 지출 건별 송금 초안 생성
        List<SettlementResponseDto.TransferDraft> drafts = new ArrayList<>();
        for (Expense expense : expenses) {
            Long payer = expense.getPaidBy();
            for (ExpenseShare share : expense.getShares()) {
                // 자기 자신이 낸 것은 제외
                if (Objects.equals(share.getMemberId(), payer)) {
                    continue;
                }
                // 분담액이 0보다 큰 경우에만 송금 초안 생성
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

        // 3. 응답
        return SettlementResponseDto.builder()
                .balances(balances)
                .transfersDraft(drafts)
                .build();
    }
}