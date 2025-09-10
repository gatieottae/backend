package com.gatieottae.backend.api.poll.dto;

import java.util.Map;

/**
 * WS로 브로드캐스트 되는 페이로드.
 * counts 는 optionId -> count 스냅샷.
 * status 는 "OPEN"|"CLOSED" 등. (필요 없으면 제거 가능)
 */
public class PollWsDto {
    public record VoteSnapshot(long pollId, String status, Map<Long, Long> counts) {}
}