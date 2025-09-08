package com.gatieottae.backend.api.poll.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class PollVoteDto {

    @Schema(description = "투표 요청", example = "{ \"optionId\": 3 }")
    public record VoteReq(Long optionId) {}

    @Schema(description = "투표 응답", example = "{ \"pollId\": 1, \"optionId\": 3, \"memberId\": 2 }")
    public record VoteRes(Long pollId, Long optionId, Long memberId) {}
}