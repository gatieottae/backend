package com.gatieottae.backend.api.group.dto;

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
    private String name;

    private String description;
}