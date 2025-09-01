package com.gatieottae.backend.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.*;

/**
 * 카카오 OAuth 로그인 요청/응답 DTO
 *
 * v1: 서버가 전달받은 authorizationCode 또는 accessToken 으로
 *     카카오 사용자 정보를 확보한 뒤 내부 Member 매핑/생성 후 JWT 발급.
 */
public class KakaoDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "KakaoLoginRequest", description = "카카오 OAuth 로그인 요청")
    public static class KakaoLoginRequest {

        @Schema(description = "카카오에서 받은 Authorization Code (accessToken 대신 보낼 수 있음)", example = "Q1w2E3r4...")
        private String authorizationCode;

        @Schema(description = "카카오 Access Token (authorizationCode 대신 보낼 수 있음)", example = "ya29.a0Af...")
        private String accessToken;

        @Schema(description = "(옵션) Authorization Code 플로우에서 사용한 redirectUri", example = "https://your.app/callback/kakao")
        private String redirectUri;

        /**
         * 유효성 규칙:
         * - authorizationCode, accessToken 중 하나는 반드시 있어야 함
         * - 둘 다 보내는 것도 허용(실무에선 보통 하나만), 최소 한 개가 비어있지 않으면 OK
         */
        @AssertTrue(message = "authorizationCode 또는 accessToken 중 하나는 반드시 필요합니다.")
        public boolean isEitherCodeOrTokenPresent() {
            return (authorizationCode != null && !authorizationCode.isBlank())
                    || (accessToken != null && !accessToken.isBlank());
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(name = "KakaoLoginResponse", description = "카카오 OAuth 로그인 결과(JWT)")
    public static class KakaoLoginResponse {

        @Schema(description = "토큰 타입", example = "Bearer")
        private final String tokenType;

        @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        private final String accessToken;

        @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        private final String refreshToken;

        public static KakaoLoginResponse of(String accessToken, String refreshToken) {
            return KakaoLoginResponse.builder()
                    .tokenType("Bearer")
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        }
    }
}