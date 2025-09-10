package com.gatieottae.backend.config;

import com.gatieottae.backend.websocket.StompAuthChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    public WebSocketConfig(StompAuthChannelInterceptor stompAuthChannelInterceptor) {
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
    }


    // 구독 경로(/topic/**)는 심플 브로커로 라우팅
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        // 클라이언트 → 서버(@MessageMapping) 목적지 prefix
        config.setApplicationDestinationPrefixes("/app");
    }

    // 핸드셰이크 엔드포인트 등록 (SockJS 포함)
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws", "/ws-stomp")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 🔑 CONNECT 단계에서 JWT 검증
        registration.interceptors(stompAuthChannelInterceptor);
    }

}