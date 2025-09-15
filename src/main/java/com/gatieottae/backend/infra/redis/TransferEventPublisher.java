package com.gatieottae.backend.infra.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatieottae.backend.api.transfer.dto.TransferWsDto;
import com.gatieottae.backend.infra.notification.NotificationTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransferEventPublisher {

    private final StringRedisTemplate redis;
    private final ObjectMapper om;

    /** 그룹 단위로 transfers 이벤트 발행 */
    public void publish(TransferWsDto payload) {
        try {
            String channel = NotificationTopics.transfersTopic(payload.getGroupId());
            String json = om.writeValueAsString(payload);
            redis.convertAndSend(channel, json);
            log.debug("[Transfers] publish channel={} payload={}", channel, json);
        } catch (Exception e) {
            // 알림은 비핵심: 로깅만 하고 비즈니스 트랜잭션에 영향 주지 않음
            log.warn("[Transfers] failed to publish", e);
        }
    }
}