package com.gatieottae.backend.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yml 의 jwt.* 바인딩 전용 */
@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private Expiration expiration = new Expiration();
    @Getter @Setter public static class Expiration {
        private long access;
        private long refresh;
    }
}