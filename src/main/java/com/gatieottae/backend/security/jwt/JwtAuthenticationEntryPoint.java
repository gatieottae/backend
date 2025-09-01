package com.gatieottae.backend.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatieottae.backend.common.exception.ApiErrorResponse;
import com.gatieottae.backend.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증이 필요한 리소스에 인증 없이 접근했을 때(또는 토큰 불량) 401 JSON을 내려주는 EntryPoint.
 * - 스프링의 ObjectMapper 빈(이미 JavaTimeModule 등록됨)을 주입받아 사용
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper; // 스프링이 관리하는 Mapper

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         org.springframework.security.core.AuthenticationException authException)
            throws IOException {

        ApiErrorResponse body = ApiErrorResponse.of(ErrorCode.UNAUTHORIZED);
        response.setStatus(ErrorCode.UNAUTHORIZED.status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}