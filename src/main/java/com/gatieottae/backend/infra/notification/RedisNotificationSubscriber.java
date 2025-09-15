package com.gatieottae.backend.infra.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatieottae.backend.api.notification.dto.NotificationPayloadDto;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisNotificationSubscriber implements MessageListener {

    private final RedisMessageListenerContainer container; // ✅ RedisConfig 에서 제공
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper om;

    @PostConstruct
    void subscribe() {
        container.addMessageListener(this, new PatternTopic(NotificationTopics.PATTERN_ALL));
        log.info("[Notif] Subscribed to {}", NotificationTopics.PATTERN_ALL);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            final String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            final String json    = new String(message.getBody(), StandardCharsets.UTF_8);

            // JSON → DTO
            NotificationPayloadDto payload = om.readValue(json, NotificationPayloadDto.class);

            // 채널 라우팅: 그룹 or 유저
            if (channel.startsWith("notif:group:")) {
                Long groupId = Long.parseLong(channel.substring("notif:group:".length()));
                // 그룹 구독자 모두에게 브로드캐스트
                String dest = "/topic/groups/" + groupId + "/notifications";
                messagingTemplate.convertAndSend(dest, payload);
            } else if (channel.startsWith("notif:user:")) {
                Long memberId = Long.parseLong(channel.substring("notif:user:".length()));
                // 개별 유저 큐(유저 전용 큐 사용)
                String dest = "/user/" + memberId + "/queue/notifications";
                messagingTemplate.convertAndSend(dest, payload);
            }
        } catch (Exception e) {
            log.debug("[Notif] failed handle pubsub: {}", e.toString());
        }
    }
}