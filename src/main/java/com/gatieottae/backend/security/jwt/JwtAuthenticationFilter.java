package com.gatieottae.backend.security.jwt;

import com.gatieottae.backend.domain.member.Member;
import com.gatieottae.backend.domain.member.MemberStatus;
import com.gatieottae.backend.repository.member.MemberRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String token = resolveToken(request); // ← 헤더 또는 쿠키

        if (StringUtils.hasText(token)) {
            try {
                // 1) 토큰 검증
                jwtTokenProvider.parseClaims(token);

                // 2) 식별자 추출
                String username = jwtTokenProvider.getUsername(token);
                Long memberId = jwtTokenProvider.getMemberId(token);

                // 3) 사용자 상태 확인
                Member m = memberRepository.findByUsername(username).orElse(null);
                if (m == null || !MemberStatus.ACTIVE.equals(m.getStatus())) {
                    SecurityContextHolder.clearContext();
                    chain.doFilter(request, response);
                    return;
                }

                // 4) 인증 컨텍스트 세팅
                AbstractAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                username, null, Collections.emptyList());
                auth.setDetails(memberId);
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        // 1) Authorization: Bearer ...
        String auth = request.getHeader("Authorization");
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            return auth.substring(7).trim();
        }
        // 2) HttpOnly 쿠키: accessToken
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("accessToken".equals(c.getName())) {
                    String v = c.getValue();
                    if (StringUtils.hasText(v)) return v.trim();
                }
            }
        }
        return null;
    }
}