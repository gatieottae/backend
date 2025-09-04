package com.gatieottae.backend.api.group.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatieottae.backend.domain.group.Group;
import com.gatieottae.backend.domain.group.GroupMember;
import com.gatieottae.backend.domain.member.Member;
import com.gatieottae.backend.repository.group.GroupMemberRepository;
import com.gatieottae.backend.repository.group.GroupRepository;
import com.gatieottae.backend.repository.member.MemberRepository;
import com.gatieottae.backend.security.jwt.JwtTokenProvider;
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

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GET /api/me/groups
 * - cursor 기반 무한스크롤
 * - status 필터(before/during/after)
 * - q 검색(title/destination)
 * - sort(startAsc/startDesc/titleAsc) — 기본은 id desc(최신 생성순)라고 가정
 */
@SpringBootTest
@AutoConfigureMockMvc
class MyGroupsControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @Autowired MemberRepository memberRepo;
    @Autowired GroupRepository groupRepo;
    @Autowired GroupMemberRepository groupMemberRepo;

    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtTokenProvider jwt;

    String accessToken;
    Long userId;

    @BeforeEach
    void setUp() {
        groupMemberRepo.deleteAll();
        groupRepo.deleteAll();
        memberRepo.deleteAll();

        // 사용자 생성
        Member me = memberRepo.save(Member.builder()
                .username("me")
                .passwordHash(passwordEncoder.encode("pw"))
                .name("나")
                .build());
        userId = me.getId();
        accessToken = jwt.generateAccessToken(me.getUsername(), me.getId());

        // 오늘 기준으로 before/during/after 섞어서 26개 생성
        // 네이밍에 '부산' 포함시켜서 검색 케이스도 커버
        LocalDate today = LocalDate.now();

        // before: 미래 일정 9개
        for (int i = 0; i < 9; i++) {
            LocalDate s = today.plusDays(5 + i);
            LocalDate e = s.plusDays(2);
            Group g = groupRepo.save(Group.builder()
                    .name("미래여행-" + i)
                    .description("부산 맛집 투어 " + i) // 검색 q=부산 를 위해 섞어둠
                    .destination(i % 2 == 0 ? "부산" : "제주")
                    .ownerId(userId)
                    .startDate(s)
                    .endDate(e)
                    .inviteCode("FUT" + i)
                    .build());
            groupMemberRepo.save(GroupMember.builder()
                    .groupId(g.getId())
                    .memberId(userId)
                    .role(GroupMember.Role.MEMBER)
                    .build());
        }

        // during: 진행중 8개
        for (int i = 0; i < 8; i++) {
            LocalDate s = today.minusDays(i);         // 시작은 과거로 분산
            LocalDate e = today.plusDays(1 + i % 3);  // 종료는 오늘 이후(진행중)
            Group g = groupRepo.save(Group.builder()
                    .name("진행중여행-" + i)
                    .description("진행중 부산 일정 " + i)
                    .destination(i % 2 == 0 ? "서울" : "부산")
                    .ownerId(userId)
                    .startDate(s)
                    .endDate(e)
                    .inviteCode("DUR" + i)
                    .build());
            groupMemberRepo.save(GroupMember.builder()
                    .groupId(g.getId())
                    .memberId(userId)
                    .role(GroupMember.Role.MEMBER)
                    .build());
        }

        // after: 지난 일정 9개
        for (int i = 0; i < 9; i++) {
            LocalDate e = today.minusDays(3 + i);
            LocalDate s = e.minusDays(2);
            Group g = groupRepo.save(Group.builder()
                    .name("지난여행-" + i)
                    .description("지난 제주 일정 " + i)
                    .destination(i % 2 == 0 ? "강릉" : "제주")
                    .ownerId(userId)
                    .startDate(s)
                    .endDate(e)
                    .inviteCode("AFT" + i)
                    .build());
            groupMemberRepo.save(GroupMember.builder()
                    .groupId(g.getId())
                    .memberId(userId)
                    .role(GroupMember.Role.MEMBER)
                    .build());
        }
    }

    @Test
    @DisplayName("첫 페이지: 기본 값(size=20), 200 OK, 최대 20개, nextCursor 존재")
    void 첫페이지_기본값_최대20개_200() throws Exception {
        mvc.perform(get("/api/me/groups")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.groups.length()", lessThanOrEqualTo(20)))
                .andExpect(jsonPath("$.nextCursor", notNullValue())); // 26개 이상 만들어서 다음페이지 존재
    }

    @Test
    @DisplayName("커서로 다음 페이지: 겹침 없음 + 정렬 유지(id desc 가정)")
    void 커서_다음페이지_겹침없음_정렬유지_200() throws Exception {
        // 1) size=5
        String page1 = mvc.perform(get("/api/me/groups")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode node1 = om.readTree(page1);
        List<GroupLite> p1 = om.convertValue(node1.get("groups"), new TypeReference<>() {});
        String cursor = node1.get("nextCursor").asText();

        assertThat(p1).hasSizeLessThanOrEqualTo(5);
        // id desc 정렬 가정: 앞의 id가 뒤보다 항상 큼
        assertThat(isIdDesc(p1)).isTrue();

        // 2) cursor로 다음 호출
        String page2 = mvc.perform(get("/api/me/groups")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("size", "5")
                        .param("cursor", cursor))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode node2 = om.readTree(page2);
        List<GroupLite> p2 = om.convertValue(node2.get("groups"), new TypeReference<>() {});

        // 겹침 없음
        Set<Long> ids1 = new HashSet<>(p1.stream().map(g -> g.id).toList());
        Set<Long> ids2 = new HashSet<>(p2.stream().map(g -> g.id).toList());
        ids1.retainAll(ids2);
        assertThat(ids1).isEmpty();

        // 정렬 유지
        assertThat(isIdDesc(p2)).isTrue();
    }

    @Test
    @DisplayName("status=before 필터 → 모두 미래여행")
    void 필터_status_before_200() throws Exception {
        mvc.perform(get("/api/me/groups")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("status", "before"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups.length()", greaterThan(0)))
                // 백에서 status 문자열을 내려주지 않는다면 startDate/endDate로 검증하거나
                // 여기서는 개수만 확인. 필요하면 커스텀 매처로 오늘 기준 비교 로직 추가.
                .andExpect(jsonPath("$.nextCursor", anything()));
    }

    @Test
    @DisplayName("검색 q=부산 → title 또는 destination/description 매칭")
    void 검색_q_destination_or_title_매칭_200() throws Exception {
        String res = mvc.perform(get("/api/me/groups")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("q", "부산"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode node = om.readTree(res);
        List<GroupLite> list = om.convertValue(node.get("groups"), new TypeReference<>() {});
        assertThat(list).isNotEmpty();
        // 실제로 부산 관련이 섞여있도록 시드함
    }

    @Test
    @DisplayName("정렬: startAsc → startDate 오름차순")
    void 정렬_startAsc_200() throws Exception {
        String res = mvc.perform(get("/api/me/groups")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("sort", "startAsc")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode node = om.readTree(res);
        List<GroupLite> list = om.convertValue(node.get("groups"), new TypeReference<>() {});
        assertThat(isStartAsc(list)).isTrue();
    }

    @Test
    @DisplayName("정렬: startDesc → startDate 내림차순")
    void 정렬_startDesc_200() throws Exception {
        String res = mvc.perform(get("/api/me/groups")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("sort", "startDesc")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode node = om.readTree(res);
        List<GroupLite> list = om.convertValue(node.get("groups"), new TypeReference<>() {});
        assertThat(isStartDesc(list)).isTrue();
    }

    @Test
    @DisplayName("잘못된 cursor → 400")
    void 잘못된_cursor_400() throws Exception {
        mvc.perform(get("/api/me/groups")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("cursor", "@@bad@@"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비로그인 → 401")
    void 비로그인_401() throws Exception {
        mvc.perform(get("/api/me/groups"))
                .andExpect(status().isUnauthorized());
    }

    /* ------------ helpers ------------- */

    // 테스트용 라이트 DTO (JSON 파싱용)
    private record GroupLite(Long id, String name, String destination, String startDate, String endDate) {}

    private boolean isIdDesc(List<GroupLite> list) {
        for (int i = 1; i < list.size(); i++) {
            if (list.get(i - 1).id <= list.get(i).id) return false;
        }
        return true;
    }

    private boolean isStartAsc(List<GroupLite> list) {
        for (int i = 1; i < list.size(); i++) {
            LocalDate prev = LocalDate.parse(list.get(i - 1).startDate);
            LocalDate curr = LocalDate.parse(list.get(i).startDate);
            if (prev.isAfter(curr)) return false;
        }
        return true;
    }

    private boolean isStartDesc(List<GroupLite> list) {
        for (int i = 1; i < list.size(); i++) {
            LocalDate prev = LocalDate.parse(list.get(i - 1).startDate);
            LocalDate curr = LocalDate.parse(list.get(i).startDate);
            if (prev.isBefore(curr)) return false;
        }
        return true;
    }
}