package com.gatieottae.backend.security.jwt;

import com.gatieottae.backend.domain.member.Member;
import com.gatieottae.backend.domain.member.MemberStatus;
import com.gatieottae.backend.repository.member.MemberRepository;

import com.gatieottae.backend.security.auth.LoginMember;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

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
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // 1) 요청 헤더(Authorization: Bearer ...) 또는 쿠키에서 JWT 추출
        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                // 2) 토큰 파싱 및 유효성 검증 (서명/만료)
                jwtTokenProvider.parseClaims(token);

                // 3) 토큰 클레임에서 username, memberId 추출
                String username = jwtTokenProvider.getUsername(token);
                Long memberId   = jwtTokenProvider.getMemberId(token);

                // 4) DB에서 사용자 조회 (id 기준)
                Member m = memberRepository.findById(memberId).orElse(null);

                // 4-1) 회원이 없거나 비활성 상태면 SecurityContext 비움
                if (m == null || !MemberStatus.ACTIVE.equals(m.getStatus())) {
                    SecurityContextHolder.clearContext();
                    chain.doFilter(request, response);
                    return;
                }

                // 4-2) 토큰 username 과 DB username 불일치 → 위조 가능성 → 거부
                if (!m.getUsername().equals(username)) {
                    SecurityContextHolder.clearContext();
                    chain.doFilter(request, response);
                    return;
                }

                // 5) principal 세팅: 커스텀 LoginMember 사용, ROLE_USER 기본 부여
                List<SimpleGrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority("ROLE_USER"));
                LoginMember principal = new LoginMember(memberId, username, authorities);

                AbstractAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

                // 6) SecurityContext 에 인증 객체 저장
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (JwtException | IllegalArgumentException e) {
                // 토큰 파싱/검증 실패 시 인증 컨텍스트 초기화
                SecurityContextHolder.clearContext();
            }
        }

        // 7) 다음 필터로 체인 진행
        chain.doFilter(request, response);
    }

    /**
     * Authorization 헤더나 accessToken 쿠키에서 JWT 추출
     */
    private String resolveToken(HttpServletRequest request) {
        // 1. 헤더 우선
        String auth = request.getHeader("Authorization");
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            return auth.substring(7).trim();
        }
        // 2. 쿠키 fallback
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