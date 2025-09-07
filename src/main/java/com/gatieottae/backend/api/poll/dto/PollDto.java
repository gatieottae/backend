package com.gatieottae.backend.api.poll.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

public class PollDto {

    @Schema(description = "투표 생성 요청")
    public record CreateReq(
            @Schema(description="그룹 ID", example="2") Long groupId,
            @Schema(description="카테고리 코드", example="FOOD") String categoryCode,
            @Schema(description="제목", example="저녁식사 어디 갈까요?") String title,
            @Schema(description="설명", example="근처에서 오픈런 가능한 곳 위주") String description,
            @Schema(description="마감 시각(선택)", example="2025-09-10T18:00:00+09:00") OffsetDateTime closesAt,
            @Schema(description="선택지", example="[\"흑돼지\", \"해물탕\", \"초밥\"]") List<String> options
    ) {}

    @Schema(description = "투표 생성 응답")
    public record CreateRes(
            @Schema(description="투표 ID", example="10") Long id
    ) {}

    @Schema(description = "투표하기 요청")
    public record VoteReq(
            @Schema(description="선택한 옵션 ID", example="101") Long optionId
    ) {}

    @Schema(description = "결과 조회 응답")
    public record ResultsRes(
            Long pollId,
            String title,
            String categoryCode,
            String status,
            OffsetDateTime closesAt,
            List<OptionResult> options,
            @Schema(description="내가 이 투표에 이미 참여했는지") boolean voted
    ) {
        public record OptionResult(
                Long optionId,
                String content,
                long votes,
                @Schema(description="내가 고른 옵션인지") boolean isMine
        ) {}
    }
}