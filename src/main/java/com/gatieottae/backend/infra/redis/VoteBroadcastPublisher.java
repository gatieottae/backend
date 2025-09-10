package com.gatieottae.backend.infra.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatieottae.backend.api.poll.dto.PollWsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoteBroadcastPublisher {

    private final StringRedisTemplate redis;
    private final ObjectMapper om;

    /** Redis Pub/Sub 채널 네이밍: polls:{pollId} */
    private static String channel(long pollId) {
        return "polls:" + pollId;
    }

    /**
     * 현재 스냅샷을 Redis Pub/Sub 로 전파.
     * - 모든 인스턴스가 이 채널을 구독하고 있으므로, 각 인스턴스에서 WS 브로드캐스트가 일어남.
     */
    public void publishSnapshot(long pollId, String status, Map<Long, Long> counts) {
        try {
            var payload = new PollWsDto.VoteSnapshot(pollId, status, counts);
            String json = om.writeValueAsString(payload);
            redis.convertAndSend(channel(pollId), json);
        } catch (Exception e) {
            log.warn("failed to publish vote snapshot. pollId={}", pollId, e);
        }
    }
}