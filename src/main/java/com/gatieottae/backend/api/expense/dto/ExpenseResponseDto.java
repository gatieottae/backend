package com.gatieottae.backend.api.expense.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ExpenseResponseDto {
    private Long id;
    private Long groupId;
    private String title;
    private Long amount;
    private Long paidBy;
    private OffsetDateTime paidAt;
    private List<ShareDto> shares;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ShareDto {
        private Long memberId;
        private Long shareAmount;
    }
}