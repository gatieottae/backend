package com.gatieottae.backend.api.settlement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SettlementResponseDto {

    @Schema(description = "멤버별 잔액표 (memberId -> 잔액[원])")
    private Map<Long, Long> balances;

    @Schema(description = "송금 초안 목록 (음수→양수 매칭)")
    private List<TransferDraft> transfersDraft;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TransferDraft {
        private Long fromMemberId;  // 보낼 사람(채무자)
        private Long toMemberId;    // 받을 사람(채권자)
        private Long amount;        // 원 단위
    }
}