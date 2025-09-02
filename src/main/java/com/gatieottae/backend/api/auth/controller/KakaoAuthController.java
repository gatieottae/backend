package com.gatieottae.backend.api.auth.controller;

import com.gatieottae.backend.config.KakaoOAuthProperties;
import com.gatieottae.backend.service.auth.oauth.SocialAuthService;
import com.gatieottae.backend.service.oauth.KakaoOAuthClient;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * 임시 테스트 단계(3단계)
 * - /api/auth/kakao/login-url  : 프론트가 받아서 이동
 * - /api/auth/kakao/callback   : code로 토큰 교환 후 /v2/user/me 조회 → JSON 그대로 반환(마스킹 포함)
 *
 * 다음 단계에서 이 콜백은 "우리 JWT 발급 + 리다이렉트" 방식으로 변경 예정.
 */
@RestController
@RequestMapping("/api/auth/kakao")
@RequiredArgsConstructor
public class KakaoAuthController {

    private final KakaoOAuthProperties props;
    private final KakaoOAuthClient kakao;
    private final SocialAuthService socialAuthService;

    /** 카카오 동의화면 URL 생성 (프론트는 이 URL로 이동) */
    @GetMapping("/login-url")
    public ResponseEntity<Map<String, String>> loginUrl() {
        String scope = String.join(",", props.getScopes());
        String url = "https://kauth.kakao.com/oauth/authorize" +
                "?response_type=code" +
                "&client_id=" + urlEncode(props.getClientId()) +
                "&redirect_uri=" + urlEncode(props.getRedirectUri()) +
                (scope.isBlank() ? "" : "&scope=" + urlEncode(scope));
        return ResponseEntity.ok(Map.of("authorizeUrl", url));
    }

    /**
     * 최종 콜백: code -> token -> me -> 우리 회원 매핑 -> JWT 쿠키 -> 프론트 리다이렉트(302)
     */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam("code") String code,
                                         @RequestParam(value = "state", required = false) String state,
                                         HttpServletResponse resp) {
        // 1) 카카오 토큰 교환 및 사용자 조회
        var token = kakao.exchangeToken(code);
        var me = kakao.me(token.getAccess_token());

        // 2) 우리 서비스 로그인(소셜 전용 자동가입 포함)
        var result = socialAuthService.loginWithKakao(
                me.getId(),
                (me.getKakao_account() != null ? me.getKakao_account().getEmail() : null),
                resolveNickname(me),
                resolveProfile(me)
        );

        // 3) JWT를 HttpOnly 쿠키로 세팅 (로컬은 Secure=false, Lax로 완화)
        addJwtCookies(resp, result.accessToken(), result.refreshToken());

        // 4) 프론트로 302 리다이렉트 (홈으로 이동)
        String frontendBase = "http://localhost:5173"; // TODO: yml로 분리(ex. oauth.kakao.frontend-redirect-base)
        String redirect = frontendBase + "/";

        return ResponseEntity.status(302)
                .location(URI.create(redirect))
                .build();
    }

    private static String resolveNickname(Object me) {
        // KakaoUserResponse 타입을 직접 참조하지 않고 null-safe로 추출
        try {
            var account = me.getClass().getMethod("getKakao_account").invoke(me);
            if (account != null) {
                var profile = account.getClass().getMethod("getProfile").invoke(account);
                if (profile != null) {
                    Object nick = profile.getClass().getMethod("getNickname").invoke(profile);
                    if (nick != null) return nick.toString();
                }
            }
            var props = me.getClass().getMethod("getProperties").invoke(me);
            if (props != null) {
                Object nick = props.getClass().getMethod("getNickname").invoke(props);
                if (nick != null) return nick.toString();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String resolveProfile(Object me) {
        try {
            var account = me.getClass().getMethod("getKakao_account").invoke(me);
            if (account != null) {
                var profile = account.getClass().getMethod("getProfile").invoke(account);
                if (profile != null) {
                    Object img = profile.getClass().getMethod("getProfile_image_url").invoke(profile);
                    if (img != null) return img.toString();
                }
            }
            var props = me.getClass().getMethod("getProperties").invoke(me);
            if (props != null) {
                Object img = props.getClass().getMethod("getProfile_image").invoke(props);
                if (img != null) return img.toString();
            }
        } catch (Exception ignored) {}
        return null;
    }

    // KakaoAuthController (콜백 내부에서 토큰 발급 직후)
    private void addJwtCookies(HttpServletResponse resp, String access, String refresh) {
        Cookie at = new Cookie("accessToken", access);
        at.setHttpOnly(true);
        at.setPath("/");                // 모든 경로에서 보이도록
        at.setMaxAge(60 * 60);         // 예시 1h
        // 로컬 개발: Lax + Secure=false (http에서도 전송)
        at.setSecure(false);
        at.setAttribute("SameSite", "Lax");

        Cookie rt = new Cookie("refreshToken", refresh);
        rt.setHttpOnly(true);
        rt.setPath("/api/auth");       // 리프레시 엔드포인트 범위 정도로 제한 권장
        rt.setMaxAge(60 * 60 * 24 * 14);
        rt.setSecure(false);
        rt.setAttribute("SameSite", "Lax");

        resp.addCookie(at);
        resp.addCookie(rt);
    }

    // ===== util =====
    private static String urlEncode(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
    private static String mask(String v, int keep) {
        if (v == null || v.length() <= keep) return "****";
        return v.substring(0, keep) + "****";
    }
}