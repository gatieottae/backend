package com.gatieottae.backend.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * oauth.kakao.* 프로퍼티 바인딩 전용 클래스
 * - YAML의 oauth.kakao.* 값을 타입 안정성 있게 주입
 * - 검증(@Validated)으로 필수값 누락 시 부팅 실패
 */
@Getter
@Validated
@ConfigurationProperties(prefix = "oauth.kakao")
@ToString(exclude = {"clientSecret"}) // 로그 출력 시 secret 제외
public class KakaoOAuthProperties {

    /** 카카오 REST API 키 (필수) */
    @NotBlank
    private final String clientId;

    /** 클라이언트 시크릿 (선택) */
    private final String clientSecret;

    /** 토큰 교환 URL (고정 권장) */
    @NotBlank
    private final String tokenUri;

    /** 사용자 정보 조회 URL (고정 권장) */
    @NotBlank
    private final String userInfoUri;

    /** 콜백 리다이렉트 URI (필수, 콘솔 등록값과 동일) */
    @NotBlank
    private final String redirectUri;

    /** 요청 스코프 목록 (쉼표 구분 문자열 → List 변환 권장) */
    private final List<String> scopes;

    public KakaoOAuthProperties(
            String clientId,
            String clientSecret,
            String tokenUri,
            String userInfoUri,
            String redirectUri,
            String scopes
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenUri = tokenUri;
        this.userInfoUri = userInfoUri;
        this.redirectUri = redirectUri;
        // "a,b,c" → ["a","b","c"]
        this.scopes = scopes == null || scopes.isBlank()
                ? List.of()
                : List.of(scopes.split("\\s*,\\s*"));
    }

    /** 외부로 secret을 절대 노출하지 않도록 마스킹 */
    public String clientIdMasked() {
        if (clientId == null || clientId.length() < 6) return "****";
        return clientId.substring(0, 3) + "****" + clientId.substring(clientId.length() - 3);
    }

    public boolean hasClientSecret() {
        return clientSecret != null && !clientSecret.isBlank();
    }
}