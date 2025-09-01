package com.gatieottae.backend.api.auth.dto;

import com.gatieottae.backend.domain.member.Member;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

/**
 * 회원가입 요청/응답 DTO 모음
 * - 요청은 Bean Validation으로 1차 검증
 * - 응답은 엔티티에서 안전한 필드만 노출
 */
public class SignupDto {

    /**
     * 회원가입 요청 바디
     * username/password/name 은 필수, email/nickname 은 선택
     */
    @Value
    @Builder
    public static class SignupRequest {
        @NotBlank(message = "username은 필수입니다.")
        @Size(min = 3, max = 50, message = "username은 3~50자여야 합니다.")
        String username;

        @NotBlank(message = "password는 필수입니다.")
        @Size(min = 8, max = 128, message = "password는 8자 이상이어야 합니다.")
        String password;

        @NotBlank(message = "name은 필수입니다.")
        @Size(max = 50, message = "name은 최대 50자입니다.")
        String name;

        @Size(max = 50, message = "nickname은 최대 50자입니다.")
        String nickname;

        @Email(message = "email 형식이 올바르지 않습니다.")
        @Size(max = 255, message = "email은 최대 255자입니다.")
        String email;
    }

    /**
     * 회원가입 응답 바디
     * 민감정보(비밀번호 등) 제외하고 반환
     */
    @Value
    @Builder
    public static class SignupResponse {
        Long id;
        String username;
        String name;
        String nickname;
        String email;

        public static SignupResponse from(Member m) {
            return SignupResponse.builder()
                    .id(m.getId())
                    .username(m.getUsername())
                    .name(m.getName())
                    .nickname(m.getNickname())
                    .email(m.getEmail())
                    .build();
        }
    }
}