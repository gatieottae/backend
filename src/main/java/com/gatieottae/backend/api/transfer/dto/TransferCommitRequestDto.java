package com.gatieottae.backend.api.transfer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransferCommitRequestDto {

    @Schema(description = "그룹 ID", example = "1")
    @NotNull
    private Long groupId;

    @Schema(description = "확정할 송금 초안 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Item> items;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Item {
        @NotNull private Long fromMemberId;
        @NotNull private Long toMemberId;
        @NotNull @Min(1) private Long amount;
        private String memo; // 선택
    }
}