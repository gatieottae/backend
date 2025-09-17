package com.gatieottae.backend.api.auth.controller;

import com.gatieottae.backend.api.auth.dto.LoginDto;
import com.gatieottae.backend.api.auth.dto.SignupDto;
import com.gatieottae.backend.api.auth.dto.UpdateMeRequestDto;
import com.gatieottae.backend.security.jwt.JwtTokenProvider;
import com.gatieottae.backend.service.auth.AuthService;
import com.gatieottae.backend.service.auth.MeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import com.gatieottae.backend.api.auth.dto.RefreshDto;
import com.gatieottae.backend.domain.member.Member;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Auth", description = "인증/회원가입/로그인 API")
@RestController
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final MeService meService;

    @Operation(
            summary = "회원가입",
            description = "username/password 기반 회원가입을 수행합니다. email, nickname은 선택입니다."
    )
    @ApiResponse(responseCode = "201", description = "회원가입 성공",
            content = @Content(schema = @Schema(implementation = SignupDto.SignupResponse.class)))
    @ApiResponse(responseCode = "400", description = "요청 바디 검증 실패")
    @ApiResponse(responseCode = "409", description = "username/email 중복")
    @PostMapping(value = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SignupDto.SignupResponse> signup(
            @Valid @RequestBody SignupDto.SignupRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        var res = authService.signup(request);
        return ResponseEntity.created(
                uriBuilder.path("/api/members/{id}").buildAndExpand(res.getId()).toUri()
        ).body(res);
    }

    @Operation(
            summary = "로그인",
            description = "username/password 로그인 성공 시 access/refresh 토큰을 발급합니다."
    )
    @ApiResponse(responseCode = "200", description = "로그인 성공",
            content = @Content(schema = @Schema(implementation = LoginDto.LoginResponse.class)))
    @ApiResponse(responseCode = "400", description = "검증 실패")
    @ApiResponse(responseCode = "401", description = "인증 실패 (없는 사용자/비밀번호 불일치)")
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginDto.LoginResponse> login(
            @Valid @RequestBody LoginDto.LoginRequest request
    ) {
        var res = authService.login(request);
        return ResponseEntity.ok(res);
    }

    @Operation(
            summary = "토큰 재발급",
            description = "유효한 refreshToken으로 새 accessToken을 발급합니다. (v1: refresh 회전 없음)"
    )
    @ApiResponse(
            responseCode = "200",
            description = "재발급 성공",
            content = @Content(schema = @Schema(implementation = RefreshDto.RefreshResponse.class))
    )
    @ApiResponse(responseCode = "401", description = "refreshToken 만료/위조 또는 사용자 불일치")
    @ApiResponse(responseCode = "403", description = "사용자 상태 비활성/정지 등")
    @PostMapping(value = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RefreshDto.RefreshResponse> refresh(
            @Valid @RequestBody RefreshDto.RefreshRequest request
    ) {
        // 서비스에서 검증/조회/상태 체크 후 새 Access 발급 (Refresh는 v1에서 회전하지 않음)
        LoginDto.LoginResponse svc = authService.refresh(request.getRefreshToken());

        // 컨트롤러 응답 스펙(RefreshDto)으로 변환
        RefreshDto.RefreshResponse body = RefreshDto.RefreshResponse.of(
                svc.getAccessToken(),
                svc.getRefreshToken()
        );
        return ResponseEntity.ok(body);
    }

    @GetMapping("/token")
    public ResponseEntity<
            Map<String, String>> issueTokenFromCookie(
            @AuthenticationPrincipal(expression = "id") Long userId
    ) {
        // 쿠키 기반 인증이 이미 통과한 상태라면 userId가 들어옴
        if (userId == null) return ResponseEntity.status(401).build();

        Member user = authService.getMemberById(userId);
        String access = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getId());
        String refresh = jwtTokenProvider.generateRefreshToken(user.getUsername(), user.getId());

        Map<String, String> body = new HashMap<>();
        body.put("accessToken", access);
        body.put("refreshToken", refresh);
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "내 정보 수정(이름)")
    @PutMapping("/me")
    public ResponseEntity<?> updateMe(Authentication authentication, @Valid @RequestBody UpdateMeRequestDto req) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of(
                    "code", "UNAUTHORIZED",
                    "status", 401,
                    "message", "로그인이 필요합니다."
            ));
        }
        String username = authentication.getName();
        Member updated = meService.updateName(username, req.name());
        return ResponseEntity.ok(new MeResponse(updated.getId(), updated.getUsername(), updated.getName(), updated.getNickname(), updated.getEmail()));
    }

    public record MeResponse(Long id, String username, String name, String nickname, String email) {}
    // UpdateMeRequest는 위에서 정의한 record 사용
}