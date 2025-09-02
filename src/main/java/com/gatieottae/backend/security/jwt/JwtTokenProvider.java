package com.gatieottae.backend.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * JWT 발급/검증 유틸리티
 * - Access/Refresh 각각 만료시간 다르게 적용
 * - subject = username, memberId는 커스텀 클레임("mid")
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTtlMillis;
    private final long refreshTtlMillis;
    private static final String ISSUER = "gatieottae";
    private static final String CLAIM_MEMBER_ID = "mid";

    public JwtTokenProvider(JwtProperties props) {
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
        this.accessTtlMillis = props.getExpiration().getAccess();
        this.refreshTtlMillis = props.getExpiration().getRefresh();
    }

    /** Access Token 발급 */
    public String generateAccessToken(String username, Long memberId) {
        return buildToken(username, memberId, accessTtlMillis);
    }

    /** Refresh Token 발급 */
    public String generateRefreshToken(String username, Long memberId) {
        return buildToken(username, memberId, refreshTtlMillis);
    }

    /** 공통 토큰 빌드 */
    private String buildToken(String username, Long memberId, long ttlMillis) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(ttlMillis);

        return Jwts.builder()
                .issuer(ISSUER)
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claims(Map.of(CLAIM_MEMBER_ID, memberId))
                .signWith(key)
                .compact();
    }

    /** Claims 파싱(시그니처/만료 검증 포함) */
    public Claims parseClaims(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 토큰 유효성 검사 */
    public boolean validate(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    /** username(subject) 추출 */
    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /** memberId(mid) 추출 */
    public Long getMemberId(String token) {
        Object v = parseClaims(token).get(CLAIM_MEMBER_ID);
        return (v instanceof Integer) ? ((Integer) v).longValue() : (Long) v;
    }
}