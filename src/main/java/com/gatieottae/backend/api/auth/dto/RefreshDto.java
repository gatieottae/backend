package com.gatieottae.backend.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * 토큰 재발급(Refresh) 요청/응답 DTO
 * v1:
 *  - 유효한 refreshToken 이면 새 accessToken 만 재발급
 *  - refreshToken 은 회전하지 않고 그대로 반환 (Rotation 은 다음 단계 고려)
 */
public class RefreshDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "TokenRefreshRequest", description = "리프레시 토큰을 이용한 액세스 토큰 재발급 요청")
    public static class RefreshRequest {
        @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        @NotBlank(message = "refreshToken 은 필수입니다.")
        private String refreshToken;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(name = "TokenRefreshResponse", description = "액세스 토큰 재발급 응답")
    public static class RefreshResponse {
        @Schema(description = "토큰 타입", example = "Bearer")
        private final String tokenType;

        @Schema(description = "새로 발급된 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        private final String accessToken;

        @Schema(description = "기존 리프레시 토큰 (v1에서는 회전하지 않음)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        private final String refreshToken;

        public static RefreshResponse of(String accessToken, String refreshToken) {
            return RefreshResponse.builder()
                    .tokenType("Bearer")
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        }
    }
}