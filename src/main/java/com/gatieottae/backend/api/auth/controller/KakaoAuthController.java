package com.gatieottae.backend.api.auth.controller;

import com.gatieottae.backend.config.KakaoOAuthProperties;
import com.gatieottae.backend.service.oauth.KakaoOAuthClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

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
     * 임시 콜백: code -> token -> me
     * - 실제 통신 성공 여부를 눈으로 확인하기 위해 토큰 일부를 마스킹하여 반환
     * - 이후 단계에서 DB조회/회원가입/JWT쿠키 세팅/리다이렉트로 바뀐다.
     */
    @GetMapping("/callback")
    public ResponseEntity<Map<String, Object>> callback(@RequestParam("code") String code) {
        var token = kakao.exchangeToken(code);
        var me = kakao.me(token.getAccess_token());

        // 응답 마스킹
        String at = mask(token.getAccess_token(), 8);
        String email = me.getKakao_account() != null ? me.getKakao_account().getEmail() : null;
        String nickname = null, profile = null;
        if (me.getKakao_account() != null && me.getKakao_account().getProfile() != null) {
            nickname = me.getKakao_account().getProfile().getNickname();
            profile  = me.getKakao_account().getProfile().getProfile_image_url();
        }
        // 구 필드 백업
        if (nickname == null && me.getProperties() != null) nickname = me.getProperties().getNickname();
        if (profile == null && me.getProperties() != null)  profile  = me.getProperties().getProfile_image();

        return ResponseEntity.ok(Map.of(
                "token", Map.of(
                        "accessToken(masked)", at,
                        "expiresIn", token.getExpires_in()
                ),
                "user", Map.of(
                        "id", me.getId(),
                        "email", email,
                        "nickname", nickname,
                        "profileImageUrl", profile
                )
        ));
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