package com.gatieottae.backend.security.jwt;

import com.gatieottae.backend.common.exception.ErrorCode;
import com.gatieottae.backend.domain.member.Member;
import com.gatieottae.backend.domain.member.MemberStatus;
import com.gatieottae.backend.repository.member.MemberRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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

/**
 * 요청 헤더의 Bearer AccessToken을 검증하고, 성공 시 SecurityContext에 Authentication을 세팅하는 필터.
 *
 * - Authorization: Bearer <accessToken>
 * - 토큰 파싱/검증: JwtTokenProvider
 * - 사용자 상태 확인: MemberRepository (예: ACTIVE만 통과)
 *
 * 실패 시: 이 필터에서 바로 401을 쓰지 않고, 체인 진행 전에
 * SecurityContext를 비워 둡니다. 실제 401 응답은 AuthenticationEntryPoint가 책임집니다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                // 1) 시그니처/만료 검증
                jwtTokenProvider.parseClaims(token);

                // 2) subject(username) & memberId 추출
                String username = jwtTokenProvider.getUsername(token);
                Long memberId = jwtTokenProvider.getMemberId(token);

                // 3) 사용자 상태 확인 (예: ACTIVE만 인증 통과)
                Member m = memberRepository.findByUsername(username)
                        .orElse(null);
                if (m == null || !MemberStatus.ACTIVE.equals(m.getStatus())) {
                    // 인증 실패로 보고 컨텍스트 비움 → EntryPoint가 401 처리
                    SecurityContextHolder.clearContext();
                    chain.doFilter(request, response);
                    return;
                }

                // 4) 권한은 지금 단계에서 비워둠(= ROLE 없음). 필요 시 부여 가능.
                AbstractAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                username,    // principal
                                null,        // credentials(불필요)
                                Collections.emptyList()  // authorities
                        );
                auth.setDetails(memberId); // 필요하면 memberId를 details에 싣기

                // 5) 컨텍스트에 인증 성공 정보 저장
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (JwtException | IllegalArgumentException e) {
                // 토큰 불량/만료 → 인증 실패로 처리(컨텍스트 클리어)
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (!StringUtils.hasText(auth)) return null;
        if (!auth.startsWith("Bearer ")) return null;
        return auth.substring(7).trim();
    }
}