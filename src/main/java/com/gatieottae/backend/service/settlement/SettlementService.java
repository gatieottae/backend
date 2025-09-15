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

    public SettlementResponseDto calculate(Long groupId) {
        List<Expense> expenses = expenseQueryRepository.findExpensesWithSharesByGroupId(groupId);

        // 1) 잔액표 계산
        Map<Long, Long> balances = new HashMap<>();
        for (Expense e : expenses) {
            // 지불자 +
            balances.merge(e.getPaidBy(), e.getAmount(), Long::sum);
            // 분담자 -
            for (ExpenseShare s : e.getShares()) {
                balances.merge(s.getMemberId(), -s.getShareAmount(), Long::sum);
            }
        }

        // 없을 수 있는 멤버(지불만/분담만)도 모두 포함됨

        // 2) 양수/음수 분리
        List<Map.Entry<Long, Long>> creditors = balances.entrySet().stream()
                .filter(it -> it.getValue() > 0)
                .sorted((a,b) -> Long.compare(b.getValue(), a.getValue())) // 많이 받을 사람부터
                .collect(Collectors.toList());

        List<Map.Entry<Long, Long>> debtors = balances.entrySet().stream()
                .filter(it -> it.getValue() < 0)
                .sorted((a,b) -> Long.compare(a.getValue(), b.getValue())) // 많이 낼 사람(더 음수)부터
                .collect(Collectors.toList());

        // 3) 투포인터 매칭 (그리디)
        int i = 0, j = 0;
        List<SettlementResponseDto.TransferDraft> drafts = new ArrayList<>();

        while (i < debtors.size() && j < creditors.size()) {
            var d = debtors.get(i);
            var c = creditors.get(j);
            long debt = -d.getValue();   // 음수 → 양수
            long credit = c.getValue();  // 양수

            long pay = Math.min(debt, credit);
            if (pay > 0) {
                drafts.add(SettlementResponseDto.TransferDraft.builder()
                        .fromMemberId(d.getKey())
                        .toMemberId(c.getKey())
                        .amount(pay)
                        .build());
                // 잔액 갱신
                d.setValue(d.getValue() + pay);      // 음수 + pay (0으로 향함)
                c.setValue(c.getValue() - pay);      // 양수 - pay (0으로 향함)
            }

            if (d.getValue() == 0) i++;
            if (c.getValue() == 0) j++;
        }

        // 4) 응답
        // (정렬/표시용으로 balances를 memberId 오름차순으로 정리해도 됨)
        return SettlementResponseDto.builder()
                .balances(balances)
                .transfersDraft(drafts)
                .build();
    }
}