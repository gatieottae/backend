package com.gatieottae.backend.api.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ExpenseRequestDto {

    @Schema(description = "그룹 ID", example = "1")
    private Long groupId;

    @Schema(description = "지출 제목", example = "숙소 예약")
    private String title;

    @Schema(description = "총 지출 금액", example = "240000")
    private Long amount;

    @Schema(description = "지불자 memberId", example = "101")
    private Long paidBy;

    @Schema(description = "분담 내역")
    private List<ShareDto> shares;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ShareDto {
        private Long memberId;
        private Long shareAmount;
    }
}