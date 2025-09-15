package com.gatieottae.backend.api.transfer.dto;

import com.gatieottae.backend.domain.expense.TransferStatus;
import lombok.*;

import java.time.OffsetDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransferResponseDto {
    private Long id;
    private Long groupId;
    private Long fromMemberId;
    private Long toMemberId;
    private Long amount;
    private TransferStatus status;
    private String proofUrl;
    private String memo;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}