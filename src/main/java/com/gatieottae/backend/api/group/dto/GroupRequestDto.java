package com.gatieottae.backend.api.group.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 그룹 생성 요청 DTO
 * - 클라이언트가 그룹 생성 시 입력하는 데이터
 */
@Getter
@Setter
public class GroupRequestDto {

    @NotBlank(message = "그룹명은 필수입니다.")
    @Schema(example = "제주도 힐링", description = "그룹명")
    private String name;

    @Schema(example = "봄 여행 계획", description = "설명(선택)")
    private String description;

    public GroupRequestDto(String name, String description) {
        this.name = name;
        this.description = description;
    }
}