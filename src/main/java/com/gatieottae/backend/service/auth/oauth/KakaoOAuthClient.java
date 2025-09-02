package com.gatieottae.backend.service.oauth;

import com.gatieottae.backend.config.KakaoOAuthProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * 카카오 OAuth용 경량 클라이언트
 * - 1) 인가코드(code) -> 액세스 토큰 교환
 * - 2) 액세스 토큰 -> 사용자 정보 조회
 *
 * 설계 이유
 * - spring-security-oauth2-client 풀스택을 쓰지 않고, 기존 JWT 발급 구조에 최소 침습으로 연동
 * - 테스트/디버깅/에러 로깅 제어가 쉬움
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoOAuthClient {

    private final KakaoOAuthProperties props;
    private final WebClient kakaoWebClient;

    /** code로 카카오 토큰 교환 */
    public TokenResponse exchangeToken(String code) {
        try {
            return kakaoWebClient.post()
                    .uri(props.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                            .with("client_id", props.getClientId())
                            .with("redirect_uri", props.getRedirectUri())
                            .with("code", code)
                            // 시크릿이 있을 때만 전송
                            .with("client_secret", props.hasClientSecret() ? props.getClientSecret() : ""))
                    .retrieve()
                    .bodyToMono(TokenResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.warn("Kakao token exchange failed: status={}, body={}", e.getRawStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    /** 액세스 토큰으로 카카오 사용자 정보 조회 */
    public KakaoUser me(String accessToken) {
        try {
            return kakaoWebClient.get()
                    .uri(props.getUserInfoUri())
                    .headers(h -> h.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(KakaoUser.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.warn("Kakao userinfo failed: status={}, body={}", e.getRawStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    // ===== 카카오 응답 DTO =====
    @Data
    public static class TokenResponse {
        private String access_token;
        private String token_type;
        private String refresh_token;
        private Long   expires_in;
        private String scope;
        private Long   refresh_token_expires_in;
    }

    @Data
    public static class KakaoUser {
        private Long id;
        private KakaoAccount kakao_account;
        private Properties properties;

        @Data
        public static class KakaoAccount {
            private String email;
            private Profile profile;

            @Data
            public static class Profile {
                private String nickname;
                private String profile_image_url;
                private Boolean is_default_image;
            }
        }

        @Data
        public static class Properties {
            private String nickname;       // 구버전 호환
            private String profile_image;  // 구버전 호환
        }
    }
}