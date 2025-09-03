package com.gatieottae.backend.api.group.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * POST /api/groups
 * - 201 성공
 * - 409 동일 owner 내 중복 이름
 * - 다른 owner는 동일 이름 201
 */
@SpringBootTest
@AutoConfigureMockMvc
class GroupControllerCreateTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper om;

    @Autowired MemberRepository memberRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtTokenProvider jwtTokenProvider;

    Long ownerAId;
    Long ownerBId;
    String tokenA;
    String tokenB;

    @BeforeEach
    void setup() {
        memberRepository.deleteAll();

        Member a = memberRepository.save(
                Member.builder()
                        .username("userA")
                        .passwordHash(passwordEncoder.encode("pw"))
                        .name("유저A")
                        .build()
        );
        Member b = memberRepository.save(
                Member.builder()
                        .username("userB")
                        .passwordHash(passwordEncoder.encode("pw"))
                        .name("유저B")
                        .build()
        );

        ownerAId = a.getId();
        ownerBId = b.getId();

        tokenA = jwtTokenProvider.generateAccessToken(a.getUsername(), a.getId());
        tokenB = jwtTokenProvider.generateAccessToken(b.getUsername(), b.getId());
    }

    private String body(String name, String desc, String dest, LocalDate start, LocalDate end) throws Exception {
        var node = om.createObjectNode();

        // required
        node.put("name", name);
        node.put("destination", dest);

        // optional
        if (desc != null) node.put("description", desc);
        if (start != null) node.put("startDate", start.toString());
        if (end != null) node.put("endDate", end.toString());

        return om.writeValueAsString(node);
    }

    @Nested
    @DisplayName("POST /api/groups")
    class CreateGroup {

        @Test
        @DisplayName("✅ 최소 필드로 201 (destination 필수)")
        void create_minimal_201() throws Exception {
            var json = body("제주도 힐링", null, "제주도", null, null);

            mockMvc.perform(
                            post("/api/groups")
                                    .header("Authorization", "Bearer " + tokenA)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.name", is("제주도 힐링")))
                    .andExpect(jsonPath("$.destination", is("제주도")))
                    .andExpect(jsonPath("$.inviteCode", not(blankOrNullString())));
        }

        @Test
        @DisplayName("❌ 동일 owner 내 동일 이름 → 409")
        void duplicate_same_owner_409() throws Exception {
            // 1차 생성 (정상)
            mockMvc.perform(
                    post("/api/groups")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("같은이름", "first", "부산", null, null))
            ).andExpect(status().isCreated());

            // 2차 동일 이름(같은 owner) → 409
            mockMvc.perform(
                            post("/api/groups")
                                    .header("Authorization", "Bearer " + tokenA)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body("같은이름", "second", "부산", null, null))
                    )
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code", is("GROUP_NAME_DUPLICATED")));
        }

        @Test
        @DisplayName("✅ 다른 owner는 같은 이름 허용 → 각자 201")
        void same_name_different_owner_201() throws Exception {
            // A가 생성
            mockMvc.perform(
                    post("/api/groups")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("같은이름", "A가 만든 그룹", "서울", null, null))
            ).andExpect(status().isCreated());

            // B도 같은 이름으로 생성
            mockMvc.perform(
                    post("/api/groups")
                            .header("Authorization", "Bearer " + tokenB)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("같은이름", "B가 만든 그룹", "서울", null, null))
            ).andExpect(status().isCreated());
        }
    }
}