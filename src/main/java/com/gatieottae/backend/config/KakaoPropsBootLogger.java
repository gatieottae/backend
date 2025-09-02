package com.gatieottae.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * 부팅 시 프로퍼티가 정상 주입되었는지 한 번만 로그로 확인
 * - 민감값은 마스킹/비노출
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class KakaoPropsBootLogger {

    private final KakaoOAuthProperties props;

    @PostConstruct
    void logOnce() {
        log.info("[KakaoOAuth] clientId={}, redirectUri={}, scopes={}",
                props.clientIdMasked(), props.getRedirectUri(), props.getScopes());
        if (props.hasClientSecret()) {
            log.info("[KakaoOAuth] clientSecret: provided(****)");
        } else {
            log.info("[KakaoOAuth] clientSecret: (none)");
        }
    }
}