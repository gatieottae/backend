package com.gatieottae.backend.api.transfer.dto;

import com.gatieottae.backend.domain.expense.Transfer;
import com.gatieottae.backend.domain.expense.TransferStatus;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    public static TransferResponseDto fromEntity(Transfer t) {
        return TransferResponseDto.builder()
                .id(t.getId())
                .groupId(t.getGroupId())
                .fromMemberId(t.getFromMemberId())
                .toMemberId(t.getToMemberId())
                .amount(t.getAmount())
                .status(t.getStatus())
                .proofUrl(t.getProofUrl())
                .memo(t.getMemo())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}