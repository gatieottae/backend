package com.gatieottae.backend.websocket;

import com.gatieottae.backend.security.jwt.JwtTokenProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtProvider;

    public StompAuthChannelInterceptor(JwtTokenProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (SimpMessageType.CONNECT.equals(accessor.getMessageType())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);

            if (!jwtProvider.validate(token)) {
                throw new IllegalArgumentException("Invalid JWT token");
            }

            Long memberId = jwtProvider.getMemberId(token);

            Principal user = new UsernamePasswordAuthenticationToken(
                    memberId, null, List.of() // 권한 필요 시 Role 추가
            );
            accessor.setUser(user);
        }

        return message;
    }
}