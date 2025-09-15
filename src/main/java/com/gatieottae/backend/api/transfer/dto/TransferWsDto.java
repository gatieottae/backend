package com.gatieottae.backend.api.transfer.dto;

import com.gatieottae.backend.domain.expense.TransferStatus;
import lombok.*;

import java.time.OffsetDateTime;

/** 브라우저로 쏴줄 WS 페이로드 표준 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class TransferWsDto {

    public enum Type {
        REQUESTED, SENT, CONFIRMED, ROLLED_BACK, NUDGE
    }

    private Type type;             // 이벤트 유형
    private Long groupId;
    private Long fromMemberId;
    private Long toMemberId;
    private Long amount;           // 원 단위
    private TransferStatus status; // 상태 변경 시 최종 상태(선택)
    private String memo;           // 선택
    private Long actorMemberId;    // 이벤트를 수행한 사용자
    private OffsetDateTime occurredAt;
}