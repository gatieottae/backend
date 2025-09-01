package com.gatieottae.backend.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * 토큰 재발급(Refresh) API의 요청/응답 DTO 모음.
 * - Request: 클라이언트가 보유한 refreshToken을 전송
 * - Response: 유효한 refreshToken일 경우 새 accessToken을 발급하여 반환
 */
public class RefreshTokenDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @Schema(description = "로그인 시 발급받은 Refresh Token(JWT)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        @NotBlank(message = "refreshToken은 비어 있을 수 없습니다.")
        private String refreshToken;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        @Schema(description = "새로 발급된 Access Token(JWT)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        private final String accessToken;

        @Schema(description = "토큰 타입 (고정값)", example = "Bearer", defaultValue = "Bearer")
        @Builder.Default
        private final String tokenType = "Bearer";

        @Schema(description = "Access Token 만료까지 남은 시간(초)", example = "900")
        private final long expiresInSeconds;
    }
}