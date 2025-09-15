package com.gatieottae.backend.config;

import com.gatieottae.backend.infra.notification.NotificationTopics;
import com.gatieottae.backend.infra.notification.RedisNotificationSubscriber;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.*;

@Configuration
public class RedisConfig {

    /**
     * 문자열 중심 작업에 특화된 템플릿.
     * - 키/필드/값 모두 String
     * - 카운팅, HINCRBY, EXPIRE 등 집계/만료에 주로 사용
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory cf) {
        StringRedisTemplate t = new StringRedisTemplate(cf);
        t.setKeySerializer(new StringRedisSerializer());
        t.setHashKeySerializer(new StringRedisSerializer());
        t.setHashValueSerializer(new StringRedisSerializer());
        t.setValueSerializer(new StringRedisSerializer());
        return t;
    }

    /**
     * (선택) 객체를 JSON으로 저장하고 싶을 때 사용할 템플릿.
     * - 여기서는 당장 필요 없지만, 향후 캐시 구조 확장 시 유용
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, Object> t = new RedisTemplate<>();
        t.setConnectionFactory(cf);
        t.setKeySerializer(new StringRedisSerializer());
        t.setHashKeySerializer(new StringRedisSerializer());
        t.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        t.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        t.afterPropertiesSet();
        return t;
    }

    // RedisMessageListenerContainer 하나만!
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory cf) {
        var container = new RedisMessageListenerContainer();
        container.setConnectionFactory(cf);
        return container;
    }

    @Bean
    public boolean registerNotificationSubscriber(
            RedisMessageListenerContainer container,
            RedisNotificationSubscriber redisNotificationSubscriber
    ) {
        container.addMessageListener(redisNotificationSubscriber, new PatternTopic(NotificationTopics.PATTERN_ALL));
        return true;
    }
}