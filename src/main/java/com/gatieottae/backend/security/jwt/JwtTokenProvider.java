package com.gatieottae.backend.security.jwt;

import com.gatieottae.backend.config.JwtProperties;
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
 * - access/refresh 각각 만료시간이 다름
 * - subject(주체)는 보통 username 또는 memberId 중 하나를 사용
 *   여기서는 username을 subject로, memberId는 커스텀 클레임("mid")에 담는 예시
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;                 // 서명용 키
    private final long accessTtlMillis;          // Access 만료(ms)
    private final long refreshTtlMillis;         // Refresh 만료(ms)
    private static final String ISSUER = "gatieottae";
    private static final String CLAIM_MEMBER_ID = "mid"; // 커스텀 클레임 키

    public JwtTokenProvider(JwtProperties props) {
        // secret 문자열 → HMAC 서명용 SecretKey로 변환
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
        this.accessTtlMillis = props.getExpiration().getAccess();
        this.refreshTtlMillis = props.getExpiration().getRefresh();
    }

    /**
     * Access Token 생성
     * @param username 로그인 아이디(주체)
     * @param memberId 내부 식별자 (커스텀 클레임)
     */
    public String generateAccessToken(String username, Long memberId) {
        return buildToken(username, memberId, accessTtlMillis);
    }

    /**
     * Refresh Token 생성
     * @param username 로그인 아이디(주체)
     * @param memberId 내부 식별자 (커스텀 클레임)
     */
    public String generateRefreshToken(String username, Long memberId) {
        return buildToken(username, memberId, refreshTtlMillis);
    }

    /**
     * 토큰 생성 공통부
     */
    private String buildToken(String username, Long memberId, long ttlMillis) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(ttlMillis);

        return Jwts.builder()
                .issuer(ISSUER)
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claims(Map.of(CLAIM_MEMBER_ID, memberId))
                .signWith(key) // HMAC-SHA 서명
                .compact();
    }

    /**
     * 토큰 파싱(검증 포함): 시그니처/만료 검증 후 Claims 반환
     * - 실패 시 JwtException 하위 예외 던짐
     */
    public Claims parseClaims(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)     // 서명 검증
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 유효성 검사: 시그니처/만료 확인
     */
    public boolean validate(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    /** subject(username) 추출 */
    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /** 커스텀 클레임: memberId(mid) 추출 */
    public Long getMemberId(String token) {
        Object v = parseClaims(token).get(CLAIM_MEMBER_ID);
        return (v instanceof Integer) ? ((Integer) v).longValue() : (Long) v;
    }
}