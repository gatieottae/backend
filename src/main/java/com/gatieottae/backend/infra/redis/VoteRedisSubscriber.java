package com.gatieottae.backend.infra.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatieottae.backend.api.poll.dto.PollWsDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoteRedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper om;

    /**
     * RedisMessageListenerContainer 등록:
     * - "polls:*" 패턴으로 모든 투표 업데이트 수신
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory cf) {
        var container = new RedisMessageListenerContainer();
        container.setConnectionFactory(cf);
        container.addMessageListener(this, new PatternTopic("polls:*"));
        return container;
    }

    @PostConstruct
    void ready() {
        log.info("VoteRedisSubscriber is ready. Subscribed to topic 'polls:*'");
    }

    /**
     * Redis Pub/Sub 로 들어온 메시지를 /topic/polls/{id} 로 브로드캐스트
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            var payload = om.readValue(json, PollWsDto.VoteSnapshot.class);

            String destination = "/topic/polls/" + payload.pollId();
            messagingTemplate.convertAndSend(destination, payload);

            log.debug("WS broadcasted to {} - {}", destination, json);
        } catch (Exception e) {
            log.warn("failed to handle redis pubsub message", e);
        }
    }
}