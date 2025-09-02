package com.gatieottae.backend.service.auth.oauth;

import com.gatieottae.backend.domain.member.Member;
import com.gatieottae.backend.domain.member.MemberStatus;
import com.gatieottae.backend.repository.member.MemberRepository;
import com.gatieottae.backend.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class SocialAuthService {

    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider; // ✅ JwtTokenProvider 사용

    @Transactional
    public LoginResult loginWithKakao(Long kakaoUserId, String email, String nickname, String profileImage) {
        final String provider = "KAKAO";
        final String subject  = String.valueOf(kakaoUserId);

        Member member = memberRepository
                .findByOauthProviderAndOauthSubject(provider, subject)
                .orElseGet(() -> {
                    String username = provider + "_" + subject; // 유니크 규칙
                    return memberRepository.save(
                            Member.socialSignup(
                                    username,
                                    email,
                                    nickname, // name
                                    nickname,
                                    profileImage,
                                    provider,
                                    subject
                            )
                    );
                });

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new IllegalStateException("비활성화된 계정입니다. 상태=" + member.getStatus());
        }

        member.markLastLogin(OffsetDateTime.now());

        // ✅ 여기만 교체
        String accessToken  = jwtTokenProvider.generateAccessToken(member.getUsername(), member.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(member.getUsername(), member.getId());

        return new LoginResult(accessToken, refreshToken, member);
    }

    public record LoginResult(String accessToken, String refreshToken, Member member) {}
}