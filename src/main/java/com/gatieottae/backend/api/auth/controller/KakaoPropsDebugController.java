package com.gatieottae.backend.api.auth.controller;

import com.gatieottae.backend.config.KakaoOAuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * GET /api/_debug/kakao-props
 * - local, dev 프로파일에서만 활성화
 * - 비밀값 없이 현재 적용값을 확인
 */
@RestController
@RequestMapping("/api/_debug")
@Profile({"local","dev"})
@RequiredArgsConstructor
public class KakaoPropsDebugController {

    private final KakaoOAuthProperties props;

    @GetMapping("/kakao-props")
    public ResponseEntity<Map<String, Object>> show() {
        return ResponseEntity.ok(Map.of(
                "clientId", props.clientIdMasked(),
                "hasClientSecret", props.hasClientSecret(),
                "tokenUri", props.getTokenUri(),
                "userInfoUri", props.getUserInfoUri(),
                "redirectUri", props.getRedirectUri(),
                "scopes", props.getScopes()
        ));
    }
}