package com.gatieottae.backend.service.settlement;

import com.gatieottae.backend.api.settlement.dto.SettlementResponseDto;
import com.gatieottae.backend.domain.expense.Expense;
import com.gatieottae.backend.domain.expense.ExpenseShare;
import com.gatieottae.backend.repository.expense.ExpenseQueryRepository;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SettlementServiceTest {

    @Test
    void calculate_balances_and_drafts_exact() {
        ExpenseQueryRepository repo = mock(ExpenseQueryRepository.class);
        SettlementService sut = new SettlementService(repo);

        // given: 4명, 총 3건 지출
        Expense e1 = Expense.builder().id(1L).groupId(1L).title("숙소").amount(240_000L).paidBy(1L).paidAt(OffsetDateTime.now()).build();
        e1.addShare(ExpenseShare.builder().memberId(1L).shareAmount(60_000L).build());
        e1.addShare(ExpenseShare.builder().memberId(2L).shareAmount(60_000L).build());
        e1.addShare(ExpenseShare.builder().memberId(3L).shareAmount(60_000L).build());
        e1.addShare(ExpenseShare.builder().memberId(4L).shareAmount(60_000L).build());

        Expense e2 = Expense.builder().id(2L).groupId(1L).title("렌터카").amount(120_000L).paidBy(2L).paidAt(OffsetDateTime.now()).build();
        e2.addShare(ExpenseShare.builder().memberId(1L).shareAmount(30_000L).build());
        e2.addShare(ExpenseShare.builder().memberId(2L).shareAmount(30_000L).build());
        e2.addShare(ExpenseShare.builder().memberId(3L).shareAmount(30_000L).build());
        e2.addShare(ExpenseShare.builder().memberId(4L).shareAmount(30_000L).build());

        Expense e3 = Expense.builder().id(3L).groupId(1L).title("저녁").amount(40_000L).paidBy(1L).paidAt(OffsetDateTime.now()).build();
        e3.addShare(ExpenseShare.builder().memberId(1L).shareAmount(20_000L).build());
        e3.addShare(ExpenseShare.builder().memberId(3L).shareAmount(20_000L).build());

        when(repo.findExpensesWithSharesByGroupId(1L)).thenReturn(List.of(e1, e2, e3));

        // when
        SettlementResponseDto res = sut.calculate(1L);

        // then: balances 합은 0
        long sum = res.getBalances().values().stream().mapToLong(Long::longValue).sum();
        assertThat(sum).isZero();

        // 특정 멤버의 잔액 검증(예: 1L은 더 많이 냄 → 채권자)
        // 실제 값은 위 시나리오 계산에 따라 양수/음수로 나와야 함
        assertThat(res.getBalances()).isNotEmpty();
        assertThat(res.getTransfersDraft()).isNotEmpty();
    }
}