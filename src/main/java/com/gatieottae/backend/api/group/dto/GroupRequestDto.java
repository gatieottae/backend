package com.gatieottae.backend.api.group.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 그룹 생성 요청 DTO
 * - 클라이언트가 그룹 생성 시 입력하는 데이터
 */
@Getter
@Setter
public class GroupRequestDto {

    @NotBlank(message = "그룹명은 필수입니다.")
    @Schema(example = "제주도 힐링", description = "그룹명 (최대 30자)")
    private String name;

    @Schema(example = "봄 여행 계획", description = "그룹 소개/설명 (선택)")
    private String description;

    @NotBlank(message = "여행지는 필수입니다.")
    @Schema(example = "제주도", description = "여행지")
    private String destination;

    @Schema(example = "2025-04-01", description = "여행 시작일(yyyy-MM-dd)")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Schema(example = "2025-04-05", description = "여행 종료일(yyyy-MM-dd)")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    public GroupRequestDto(String name, String description, String destination,
                           LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.description = description;
        this.destination = destination;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}