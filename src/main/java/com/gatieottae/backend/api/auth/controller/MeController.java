package com.gatieottae.backend.api.auth.controller;

import com.gatieottae.backend.domain.member.Member;
import com.gatieottae.backend.repository.member.MemberRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Auth", description = "인증/회원가입/로그인/내 정보")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MeController {

    private final MemberRepository memberRepository;

    /**
     * 보호된 엔드포인트 예시.
     * - Authorization: Bearer <accessToken> 필요
     * - SecurityContext의 Authentication.getName() → username
     */
    @Operation(
            summary = "내 정보 조회"
    )
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        // 1) 인증 없음 → 401
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of(
                    "code", "UNAUTHORIZED",
                    "status", 401,
                    "message", "로그인이 필요합니다."
            ));
        }

        // 2) 인증 있음 → username으로 회원 조회
        String username = authentication.getName();
        Member m = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("회원 정보를 찾을 수 없습니다. username=" + username));

        MeResponse res = new MeResponse(
                m.getId(),
                m.getUsername(),
                m.getName(),
                m.getNickname(),
                m.getEmail()
        );
        return ResponseEntity.ok(res);
    }

    public record MeResponse(Long id, String username, String name, String nickname, String email) {}
}