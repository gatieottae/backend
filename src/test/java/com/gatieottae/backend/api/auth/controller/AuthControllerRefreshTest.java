package com.gatieottae.backend.api.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatieottae.backend.api.auth.dto.LoginDto;
import com.gatieottae.backend.api.auth.dto.RefreshDto;
import com.gatieottae.backend.common.exception.BadRequestException;
import com.gatieottae.backend.common.exception.ConflictException;
import com.gatieottae.backend.common.exception.ErrorCode;
import com.gatieottae.backend.service.auth.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ✅ 컨트롤러 slice 테스트: AuthController만 로드
@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
// ✅ 전역 예외 핸들러/메시지 컨버터 등 필요한 빈을 명시적으로 import
@Import({com.gatieottae.backend.common.exception.GlobalExceptionHandler.class})
class AuthControllerRefreshTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper om;

    // ✅ Service는 Mock으로 대체하여 컨트롤러 레이어만 검증
    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("성공: 유효한 refreshToken → 200 OK + 새 accessToken 반환")
    void refresh_success() throws Exception {
        // given: 서비스가 성공 응답을 반환하도록 스텁
        LoginDto.LoginResponse stub = LoginDto.LoginResponse.builder()
                .tokenType("Bearer")
                .accessToken("new-access")
                .refreshToken("same-refresh")
                .build();
        Mockito.when(authService.refresh("valid-refresh")).thenReturn(stub);

        RefreshDto.RefreshRequest req = new RefreshDto.RefreshRequest("valid-refresh");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.accessToken", is("new-access")))
                .andExpect(jsonPath("$.refreshToken", is("same-refresh")));
    }

    @Test
    @DisplayName("실패: 요청 바디 검증 실패(빈 refreshToken) → 400 Bad Request")
    void refresh_validation_fail() throws Exception {
        // given: refreshToken이 공백 -> @Valid @NotBlank 위반
        RefreshDto.RefreshRequest req = new RefreshDto.RefreshRequest("  ");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패: 토큰 무효/불일치 → 401 Unauthorized (BadRequestException 매핑)")
    void refresh_invalid_token() throws Exception {
        Mockito.when(authService.refresh(anyString()))
                .thenThrow(new BadRequestException(ErrorCode.UNAUTHORIZED, "유효하지 않은 토큰"));

        RefreshDto.RefreshRequest req = new RefreshDto.RefreshRequest("bad-refresh");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is(ErrorCode.UNAUTHORIZED.name())))
                .andExpect(jsonPath("$.message", not(emptyOrNullString())));
    }

    @Test
    @DisplayName("실패: 사용자 상태 이슈(BLOCKED/DELETED) → 403 Forbidden (ConflictException 매핑)")
    void refresh_forbidden() throws Exception {
        Mockito.when(authService.refresh(anyString()))
                .thenThrow(new ConflictException(ErrorCode.FORBIDDEN, "비활성 사용자"));

        RefreshDto.RefreshRequest req = new RefreshDto.RefreshRequest("valid-but-forbidden");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is(ErrorCode.FORBIDDEN.name())))
                .andExpect(jsonPath("$.message", not(emptyOrNullString())));
    }
}