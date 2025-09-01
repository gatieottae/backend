package com.gatieottae.backend.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

/**
 * 로그인 요청/응답 DTO
 * - 요청: username/password 검증만 담당(Bean Validation)
 * - 응답: 액세스/리프레시 토큰과 토큰 타입을 내려준다.
 */
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
}