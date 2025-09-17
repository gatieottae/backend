package com.gatieottae.backend.infra.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatieottae.backend.api.notification.dto.NotificationPayloadDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisNotificationPublisher {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper om;

    public void publishToUser(Long memberId, NotificationPayloadDto payload) {
        publish(NotificationTopics.userTopic(memberId), payload);
    }

    public void publishToGroup(Long groupId, NotificationPayloadDto payload) {
        publish(NotificationTopics.groupTopic(groupId), payload);
    }

    private void publish(String channel, NotificationPayloadDto payload) {
        try {
            stringRedisTemplate.convertAndSend(channel, om.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.info("[Redis] publish error: {}", e);
        }
    }
}