package com.gatieottae.backend.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * 로그인 요청/응답 DTO
 * - 요청: username/password 검증만 담당(Bean Validation)
 * - 응답: 액세스/리프레시 토큰과 토큰 타입을 내려준다.
 */
@Slf4j
public class LoginDto {

    /** 로그인 요청 바디 */
    @Value
    @Builder
    public static class LoginRequest {
        @NotBlank(message = "username은 필수입니다.")
        @Size(min = 3, max = 50, message = "username은 3~50자여야 합니다.")
        String username;

        @NotBlank(message = "password는 필수입니다.")
        @Size(min = 8, max = 128, message = "password는 8자 이상이어야 합니다.")
        String password;
    }

    /** 로그인 응답 바디 */
    @Value
    @Builder
    public static class LoginResponse {
        String tokenType;     // 일반적으로 "Bearer"
        String accessToken;   // 짧은 만료
        String refreshToken;  // 긴 만료
    }

    /**
     * 로그인/토큰 관련 DTO
     * - refresh 재발급에 필요한 Request/Response 포함
     */
    @Value
    @Builder
    public static class RefreshRequest {
        @NotBlank
        @Schema(description = "재발급용 Refresh Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6...")
        String refreshToken;
    }

    @Value
    @Builder
    public static class RefreshResponse {
        @Schema(description = "새 Access Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6...")
        String accessToken;

        @Schema(description = "Refresh Token (기존 그대로 반환, Rotation은 추후 적용)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6...")
        String refreshToken;

        @Schema(description = "토큰 타입", example = "Bearer")
        @Builder.Default
        String tokenType = "Bearer";
    }
}