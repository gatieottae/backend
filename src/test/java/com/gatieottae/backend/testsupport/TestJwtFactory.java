package com.gatieottae.backend.testsupport;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.time.Instant;
import java.util.Date;

public class TestJwtFactory {

    private final Key key;
    private final String issuer;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;

    public TestJwtFactory(String base64Secret, String issuer,
                          long accessTtlSeconds, long refreshTtlSeconds) {
        // 앱과 동일: Base64 디코드해서 HMAC 키 생성
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.issuer = issuer;
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    public String refresh(String username, Long memberId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTtlSeconds)))
                .claim("mid", memberId)   // 앱과 동일한 클레임명 사용
                .signWith(key) // ★ HS256 고정
                .compact();
    }

    public String access(String username, Long memberId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
                .claim("mid", memberId)
                .signWith(key)
                .compact();
    }
}