package com.gatieottae.backend.infra.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatieottae.backend.api.notification.dto.NotificationPayloadDto;
import com.gatieottae.backend.domain.member.Member;
import com.gatieottae.backend.repository.member.MemberRepository;
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

    private final RedisMessageListenerContainer container;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper om;
    private final MemberRepository memberRepository;

    @PostConstruct
    void subscribe() {
        container.addMessageListener(this, new PatternTopic(NotificationTopics.PATTERN_ALL)); // "notif:*"
        log.info("[Notif] Subscribed to {}", NotificationTopics.PATTERN_ALL);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            final String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            final String json    = new String(message.getBody(), StandardCharsets.UTF_8);

            NotificationPayloadDto payload = om.readValue(json, NotificationPayloadDto.class);

            if (channel.startsWith("notif:group:")) {
                Long groupId = Long.parseLong(channel.substring("notif:group:".length()));
                String dest = "/topic/groups/" + groupId + "/notifications";
                log.info("[Notif] SEND_TO_GROUP {} {}", groupId, dest);
                messagingTemplate.convertAndSend(dest, payload);

            } else if (channel.startsWith("notif:user:")) {
                Long memberId = Long.parseLong(channel.substring("notif:user:".length()));
                String dest = "/topic/notifications/" + memberId;
                log.info("[Notif] STOMP SEND {}", dest);
                messagingTemplate.convertAndSend(dest, payload);
            }
        } catch (Exception e) {
            log.warn("[Notif] failed handle pubsub", e);
        }
    }
}