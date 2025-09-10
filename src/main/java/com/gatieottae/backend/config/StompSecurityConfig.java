package com.gatieottae.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

@Configuration
@EnableWebSocketSecurity
public class StompSecurityConfig {

    // ✅ 메시징 CSRF 비활성화 (개발용)
    @Bean(name = "csrfChannelInterceptor")
    public ChannelInterceptor csrfChannelInterceptor() {
        // no-op interceptor -> CSRF 검사 안 함
        return new ChannelInterceptor() {};
    }

    @Bean
    AuthorizationManager<Message<?>> messageAuthorizationManager(
            MessageMatcherDelegatingAuthorizationManager.Builder messages
    ) {
        return messages
                // 기본 연결/하트비트/해제 허용
                .simpTypeMatchers(
                        SimpMessageType.CONNECT,
                        SimpMessageType.HEARTBEAT,
                        SimpMessageType.UNSUBSCRIBE,
                        SimpMessageType.DISCONNECT
                ).permitAll()

                // 서버 핸들러(@MessageMapping) 목적지 → 인증 필요
                .simpDestMatchers("/app/**").authenticated()

                // 브로커 구독 목적지 → 공개 허용
                .simpSubscribeDestMatchers("/topic/**", "/queue/**").permitAll()

                // 그 외는 거부
                .anyMessage().denyAll()

                .build();
    }
}