package com.gatieottae.backend.api.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatieottae.backend.domain.member.Member;
import com.gatieottae.backend.repository.member.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController /login 통합 테스트
 *
 * 구성:
 * - @SpringBootTest + @AutoConfigureMockMvc 로 컨트롤러~리포지토리 실제 구동
 * - H2 인메모리 DB에 테스트용 사용자 저장
 * - 성공/실패(401)/검증실패(400) 케이스 검증
 *
 * 주의:
 * - 서비스에서 '비밀번호 불일치'와 '존재하지 않는 사용자'는 보안상 같은 응답(401)을 권장
 * - GlobalExceptionHandler에서 401 매핑(UnauthorizedException 등)이 되어 있어야 함
 *   (아직 없다면 UnauthorizedException + 핸들러 추가 필요)
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerLoginTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MemberRepository memberRepository;
    @Autowired PasswordEncoder passwordEncoder;

    // 공통 테스트 유저
    private static final String USERNAME = "alice01";
    private static final String RAW_PASSWORD = "Passw0rd!";
    private static final String NAME = "Alice";

    @BeforeEach
    void setUp() {
        // 테스트 격리를 위해 매번 깨끗하게
        memberRepository.deleteAll();

        // 로그인 성공/실패 테스트용 사용자 1명 저장
        Member m = Member.builder()
                .username(USERNAME)
                .passwordHash(passwordEncoder.encode(RAW_PASSWORD)) // 반드시 인코딩된 해시 저장
                .name(NAME)
                .email("alice@example.com")
                .build();

        memberRepository.save(m);
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("✅ 성공: 올바른 username/password → 200 & access/refresh/tokenType 반환")
        void login_success() throws Exception {
            var body = """
                { "username": "%s", "password": "%s" }
            """.formatted(USERNAME, RAW_PASSWORD);

            mockMvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body)
                    )
                    .andExpect(status().isOk())
                    // 응답 형태: { accessToken, refreshToken, tokenType: "Bearer" }
                    .andExpect(jsonPath("$.accessToken", not(blankOrNullString())))
                    .andExpect(jsonPath("$.refreshToken", not(blankOrNullString())))
                    .andExpect(jsonPath("$.tokenType", is("Bearer")));
        }

        @Test
        @DisplayName("❌ 401: 비밀번호 불일치")
        void login_wrongPassword_401() throws Exception {
            var body = """
                { "username": "%s", "password": "WRONG_PASSWORD" }
            """.formatted(USERNAME);

            mockMvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body)
                    )
                    .andExpect(status().isUnauthorized())
                    // 에러 바디 스펙(ApiErrorResponse)을 쓰는 경우, code/status 등까지 점검 가능
                    .andExpect(jsonPath("$.code", anyOf(is("UNAUTHORIZED"), notNullValue())))
                    .andExpect(jsonPath("$.status", is(401)));
        }

        @Test
        @DisplayName("❌ 401: 존재하지 않는 사용자")
        void login_unknownUser_401() throws Exception {
            var body = """
                { "username": "not_exist_user", "password": "%s" }
            """.formatted(RAW_PASSWORD);

            mockMvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body)
                    )
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status", is(401)));
        }

        @Test
        @DisplayName("❌ 400: 검증 실패 (username 최소 길이 미만 / password 누락 등)")
        void login_validationFail_400() throws Exception {
            // username 너무 짧게(2자), password 누락
            var body = """
                { "username": "ab" }
            """;

            mockMvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body)
                    )
                    .andExpect(status().isBadRequest())
                    // 표준 에러 스펙을 쓰면 필드 에러 목록까지 검증
                    .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                    .andExpect(jsonPath("$.errors", notNullValue()));
        }
    }
}