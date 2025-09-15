package com.gatieottae.backend.infra.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatieottae.backend.api.transfer.dto.TransferWsDto;
import com.gatieottae.backend.infra.notification.NotificationTopics;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Redis Pub/Sub 구독 → 각 인스턴스의 WebSocket 세션으로 팬아웃
 * - 그룹 브로드캐스트: /topic/groups/{groupId}/transfers
 * - (옵션) 개인 알림도 함께 보낼 경우 /topic/notifications/{memberId} 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransferRedisSubscriber implements MessageListener {

    private final RedisMessageListenerContainer container; // RedisConfig 에서 제공
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper om;

    @PostConstruct
    void subscribe() {
        container.addMessageListener(this, new PatternTopic(NotificationTopics.PATTERN_TRANSFERS_ALL));
        log.info("[Transfers] Subscribed to {}", NotificationTopics.PATTERN_TRANSFERS_ALL);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            TransferWsDto payload = om.readValue(json, TransferWsDto.class);

            // 그룹 브로드캐스트
            String dest = NotificationTopics.wsTransfersDestination(payload.getGroupId());
            messagingTemplate.convertAndSend(dest, payload);
            log.debug("[Transfers] WS broadcast to {} :: {}", dest, json);

            // 선택: 개인 알림도 함께
            if (payload.getToMemberId() != null) {
                String userDest = NotificationTopics.wsUserNotificationDestination(payload.getToMemberId());
                messagingTemplate.convertAndSend(userDest, payload);
                log.debug("[Transfers] WS unicast to {} :: {}", userDest, json);
            }
        } catch (Exception e) {
            log.warn("[Transfers] failed handle pubsub", e);
        }
    }
}