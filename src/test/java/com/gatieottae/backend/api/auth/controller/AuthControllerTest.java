package com.gatieottae.backend.api.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatieottae.backend.domain.member.Member;
import com.gatieottae.backend.repository.member.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")          // 테스트는 H2 in-memory
@Transactional                  // 각 테스트 후 롤백
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired MemberRepository memberRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String json(Object body) throws Exception {
        return om.writeValueAsString(body);
    }

    private Map<String, Object> signupReq(
            String username, String password, String name,
            String nickname, String email
    ) {
        Map<String, Object> m = new HashMap<>();
        m.put("username", username);
        m.put("password", password);
        m.put("name",     name);
        if (nickname != null) m.put("nickname", nickname);
        if (email != null)    m.put("email", email);
        return m;
    }

    @Nested
    @DisplayName("POST /api/auth/signup")
    class Signup {

        @Test
        @DisplayName("성공: username/password/name로 가입하면 201과 요약 반환")
        void success() throws Exception {
            Map<String, Object> req = signupReq(
                    "alice01", "Password!123", "Alice", "앨리스", "alice@example.com"
            );

            mvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/api/members/")))
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.username").value("alice01"))
                    .andExpect(jsonPath("$.name").value("Alice"))
                    .andExpect(jsonPath("$.nickname").value("앨리스"))
                    .andExpect(jsonPath("$.email").value("alice@example.com"));
        }

        @Test
        @DisplayName("실패: username 중복이면 409(CONFLICT) + 표준 에러 응답")
        void duplicate_username() throws Exception {
            // Given: 이미 가입된 사용자
            memberRepository.save(
                    Member.builder()
                            .username("bob01")
                            .passwordHash(passwordEncoder.encode("Pw!123456"))
                            .name("Bob")
                            .email("bob@example.com")
                            .build()
            );

            Map<String, Object> req = signupReq(
                    "bob01", "Pw!123456", "Bob Jr.", null, "bobjr@example.com"
            );

            mvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isConflict())
                    // ErrorCode가 DUPLICATE_USERNAME으로 내려오면 아래와 같이 체크
                    .andExpect(jsonPath("$.code", anyOf(is("DUPLICATE_USERNAME"), is("CONFLICT"))))
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message", not(isEmptyOrNullString())));
        }

        @Test
        @DisplayName("실패: 검증 오류(username 너무 짧음, password 누락 등)면 400 + 필드 오류 목록")
        void validation_failed() throws Exception {
            Map<String, Object> req = signupReq(
                    "ab",   // 3자 미만 → 검증 실패
                    "",     // 빈 패스워드 → 검증 실패
                    "A",    // name은 OK
                    null,
                    "not-an-email" // 형식 오류
            );

            mvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.errors", not(empty())))
                    .andExpect(jsonPath("$.errors[*].field", hasItems("username", "password", "email")));
        }
    }
}