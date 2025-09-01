package com.gatieottae.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 관련 설정값을 yml에서 불러오기 위한 클래스
 * - secret: 서명 비밀키
 * - expiration.access: Access Token 만료 시간(ms)
 * - expiration.refresh: Refresh Token 만료 시간(ms)
 *
 * application.yml 예시:
 * jwt:
 *   secret: mysecretkeymysecretkeymysecretkey
 *   expiration:
 *     access: 3600000      # 1시간
 *     refresh: 1209600000  # 2주
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private Expiration expiration = new Expiration();

    public static class Expiration {
        private long access;
        private long refresh;

        public long getAccess() { return access; }
        public void setAccess(long access) { this.access = access; }

        public long getRefresh() { return refresh; }
        public void setRefresh(long refresh) { this.refresh = refresh; }
    }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public Expiration getExpiration() { return expiration; }
    public void setExpiration(Expiration expiration) { this.expiration = expiration; }
}