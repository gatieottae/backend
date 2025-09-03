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
// ✅ 변경: 권한 리스트 생성에 사용
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

        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                jwtTokenProvider.parseClaims(token);

                String username = jwtTokenProvider.getUsername(token);
                Long memberId = jwtTokenProvider.getMemberId(token);

                Member m = memberRepository.findByUsername(username).orElse(null);
                if (m == null || !MemberStatus.ACTIVE.equals(m.getStatus())) {
                    SecurityContextHolder.clearContext();
                    chain.doFilter(request, response);
                    return;
                }

                // ✅ 여기부터 변경: principal을 LoginMember로 세팅
                // 권한은 필요 시 DB/클레임에서 가져오면 됨. 우선 ROLE_USER 기본 부여.
                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
                LoginMember principal = new LoginMember(memberId, username, authorities);

                AbstractAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

                // ✅ 더 이상 details에 memberId 넣지 않음
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            return auth.substring(7).trim();
        }
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