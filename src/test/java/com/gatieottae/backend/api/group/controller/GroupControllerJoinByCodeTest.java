package com.gatieottae.backend.api.group.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gatieottae.backend.domain.member.Member;
import com.gatieottae.backend.repository.member.MemberRepository;
import com.gatieottae.backend.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.*;
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
 * POST /api/groups/join/code
 * - 200 정상 참여
 * - 404 INVALID_CODE (코드 없거나 만료)
 * - 409 ALREADY_MEMBER (이미 멤버)
 */
@SpringBootTest
@AutoConfigureMockMvc
class GroupControllerJoinByCodeTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper om;

    @Autowired MemberRepository memberRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtTokenProvider jwtTokenProvider;

    String tokenOwner;  // 그룹 생성자
    String tokenGuest;  // 초대코드로 참여할 사용자

    @BeforeEach
    void setup() throws Exception {
        memberRepository.deleteAll();

        Member owner = memberRepository.save(Member.builder()
                .username("owner")
                .passwordHash(passwordEncoder.encode("pw"))
                .name("오너")
                .build());

        Member guest = memberRepository.save(Member.builder()
                .username("guest")
                .passwordHash(passwordEncoder.encode("pw"))
                .name("게스트")
                .build());

        tokenOwner = jwtTokenProvider.generateAccessToken(owner.getUsername(), owner.getId());
        tokenGuest = jwtTokenProvider.generateAccessToken(guest.getUsername(), guest.getId());
    }

    private String createGroupAndGetInviteCode(String name, String destination) throws Exception {
        ObjectNode req = om.createObjectNode();
        req.put("name", name);
        req.put("destination", destination);
        // description/startDate/endDate는 선택
        String res = mockMvc.perform(
                        post("/api/groups")
                                .header("Authorization", "Bearer " + tokenOwner)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(om.writeValueAsString(req))
                ).andExpect(status().isCreated())
                .andExpect(jsonPath("$.inviteCode", not(blankOrNullString())))
                .andReturn().getResponse().getContentAsString();

        JsonNode node = om.readTree(res);
        return node.get("inviteCode").asText();
    }

    private String joinBody(String code) throws Exception {
        ObjectNode req = om.createObjectNode();
        req.put("code", code);
        return om.writeValueAsString(req);
    }

    @Nested
    @DisplayName("POST /api/groups/join/code")
    class JoinByCode {

        @Test
        @DisplayName("✅ 초대 코드로 정상 참여 → 200 OK")
        void join_success_200() throws Exception {
            String inviteCode = createGroupAndGetInviteCode("제주여행", "제주도");

            mockMvc.perform(
                            post("/api/groups/join/code")
                                    .header("Authorization", "Bearer " + tokenGuest)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(joinBody(inviteCode))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.name", is("제주여행")))
                    .andExpect(jsonPath("$.destination", is("제주도")));
        }

        @Test
        @DisplayName("❌ 잘못된 코드 → 404 INVALID_CODE")
        void invalid_code_404() throws Exception {
            mockMvc.perform(
                            post("/api/groups/join/code")
                                    .header("Authorization", "Bearer " + tokenGuest)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(joinBody("INVALIDXX"))   // 존재하지 않는 코드
                    )
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("INVALID_CODE")));
        }

        @Test
        @DisplayName("❌ 이미 멤버가 재참여 시도 → 409 ALREADY_MEMBER")
        void already_member_409() throws Exception {
            String inviteCode = createGroupAndGetInviteCode("부산여행", "부산");

            // 첫 참여: OK
            mockMvc.perform(
                    post("/api/groups/join/code")
                            .header("Authorization", "Bearer " + tokenGuest)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(joinBody(inviteCode))
            ).andExpect(status().isOk());

            // 재참여: 409
            mockMvc.perform(
                            post("/api/groups/join/code")
                                    .header("Authorization", "Bearer " + tokenGuest)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(joinBody(inviteCode))
                    )
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code", is("ALREADY_MEMBER")));
        }
    }
}