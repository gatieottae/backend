package com.gatieottae.backend.api.transfer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransferActionRequestDto {
    @Schema(description = "메모/사유(선택)")
    private String memo;
}