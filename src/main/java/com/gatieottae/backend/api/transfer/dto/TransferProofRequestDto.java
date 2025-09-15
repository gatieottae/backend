package com.gatieottae.backend.api.transfer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransferProofRequestDto {
    @Schema(description = "증빙 URL", example = "https://cdn.../receipt.png")
    private String proofUrl;
    @Schema(description = "증빙 메모(선택)")
    private String memo;
}