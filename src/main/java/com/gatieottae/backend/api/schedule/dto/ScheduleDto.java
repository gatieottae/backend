package com.gatieottae.backend.api.schedule.dto;

import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;

public class ScheduleDto {

    // ---------- 요청 DTO ----------
    @Schema(name = "ScheduleCreateRequest", description = "일정 생성 요청")
    public record CreateReq(
            @NotBlank @Size(min = 1, max = 128)
            @Schema(description = "제목", example = "성산일출봉 일출")
            String title,

            @Schema(description = "설명", example = "일출 30분 전 집합")
            String description,

            @Size(max = 255)
            @Schema(description = "장소", example = "성산읍 성산일출봉 주차장")
            String location,

            @NotNull
            @Schema(description = "시작시각(ISO-8601)", example = "2025-09-03T20:40:00Z")
            OffsetDateTime startTime,

            @NotNull
            @Schema(description = "종료시각(ISO-8601)", example = "2025-09-03T22:30:00Z")
            OffsetDateTime endTime
    ) {}

    @Schema(name = "AttendanceRequest", description = "참여 상태 변경 요청")
    public record AttendanceReq(
            @NotBlank
            @Pattern(regexp = "INVITED|GOING|NOT_GOING|TENTATIVE",
                    message = "status must be one of INVITED, GOING, NOT_GOING, TENTATIVE")
            @Schema(description = "참여 상태", example = "GOING")
            String status
    ) {}

    // ---------- 응답 DTO ----------
    @Schema(name = "ScheduleCreateResponse", description = "일정 생성 응답")
    public record CreateRes(
            @Schema(description = "생성된 일정 ID", example = "321")
            Long id,
            @Schema(description = "겹치는 일정 ID 목록", example = "[12,45]")
            List<Long> overlappedIds
    ) {}

    @Schema(name = "ScheduleItem", description = "캘린더 목록 아이템")
    public record Item(
            Long id,
            String title,
            String location,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            Attending attending,
            @Schema(description = "프론트 겹침 하이라이트 플래그", example = "true")
            boolean overlap
    ) {
        @Schema(name = "AttendingSummary")
        public record Attending(
                @Schema(description = "참여 인원 수", example = "5")
                long count,
                @Schema(description = "표시할 샘플 목록(최대 2)", example = """
        [{"memberId":11,"displayName":"민수"},{"memberId":14,"displayName":"지현"}]
        """)
                List<Member> sample,
                @Schema(description = "숨겨진 인원 존재 여부", example = "true")
                boolean hasMore,
                @Schema(description = "내가 참여중인지", example = "true")
                boolean isMine
        ) {}

        @Schema(name = "AttendingMember")
        public record Member(
                Long memberId,
                String displayName
        ) {}
    }

    public record UpdateReq(
            String title,
            String description,
            String location,
            OffsetDateTime startTime,
            OffsetDateTime endTime
    ) {}
}