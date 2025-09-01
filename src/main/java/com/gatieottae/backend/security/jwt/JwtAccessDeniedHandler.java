package com.gatieottae.backend.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatieottae.backend.common.exception.ApiErrorResponse;
import com.gatieottae.backend.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증은 됐지만 권한이 부족할 때 403 JSON을 내려주는 핸들러.
 */
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper om = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        ApiErrorResponse body = ApiErrorResponse.of(ErrorCode.FORBIDDEN);
        response.setStatus(ErrorCode.FORBIDDEN.status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        om.writeValue(response.getOutputStream(), body);
    }
}