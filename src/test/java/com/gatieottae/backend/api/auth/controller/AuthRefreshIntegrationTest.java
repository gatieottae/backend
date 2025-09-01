package com.gatieottae.backend.api.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatieottae.backend.api.auth.dto.RefreshDto;
import com.gatieottae.backend.domain.member.Member;
import com.gatieottae.backend.domain.member.MemberStatus;
import com.gatieottae.backend.repository.member.MemberRepository;
import com.gatieottae.backend.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthRefreshIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper om;

    @Autowired MemberRepository memberRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;

    private Member saveMember(String username, MemberStatus status) {
        Member m = Member.builder()
                .username(username)
                .passwordHash("$2a$10$dummy") // 실제 검증은 안함
                .name("테스터")
                .status(status)
                .build();
        return memberRepository.save(m);
    }

    private String makeRefresh(Member m) {
        // ✅ 실제 Bean으로 refresh 토큰 발급
        return jwtTokenProvider.generateRefreshToken(m.getUsername(), m.getId());
    }

    @Nested
    class Success {

        @Test
        @DisplayName("성공: 유효한 refreshToken → 200 OK + 새 accessToken 반환")
        void refresh_success() throws Exception {
            Member active = saveMember("alice", MemberStatus.ACTIVE);
            String refresh = makeRefresh(active);

            RefreshDto.RefreshRequest req = new RefreshDto.RefreshRequest(refresh);

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.tokenType", is("Bearer")))
                    .andExpect(jsonPath("$.accessToken", not(emptyOrNullString())))
                    .andExpect(jsonPath("$.refreshToken", is(refresh)));
        }
    }

    @Nested
    class Failures {

        @Test
        @DisplayName("실패: 요청 바디 검증 실패(빈 refreshToken) → 400")
        void refresh_validation_fail() throws Exception {
            RefreshDto.RefreshRequest req = new RefreshDto.RefreshRequest("  ");
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 토큰 위조/무효 → 401")
        void refresh_invalid_token() throws Exception {
            RefreshDto.RefreshRequest req = new RefreshDto.RefreshRequest("bad-refresh");
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHORIZED")))
                    .andExpect(jsonPath("$.message", not(emptyOrNullString())));
        }

        @Test
        @DisplayName("실패: 사용자 BLOCKED 상태 → 403")
        void refresh_blocked_user() throws Exception {
            Member blocked = saveMember("bob", MemberStatus.BLOCKED);
            String refresh = makeRefresh(blocked);

            RefreshDto.RefreshRequest req = new RefreshDto.RefreshRequest(refresh);
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(req)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("FORBIDDEN")))
                    .andExpect(jsonPath("$.message", not(emptyOrNullString())));
        }
    }
}