package com.gatieottae.backend.service.auth;

import com.gatieottae.backend.api.auth.dto.SignupDto;
import com.gatieottae.backend.common.exception.ConflictException;
import com.gatieottae.backend.common.exception.ErrorCode;
import com.gatieottae.backend.domain.member.Member;
import com.gatieottae.backend.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 인증 관련 쓰기(use-case) 서비스
 * - 회원가입(Signup): DTO → 엔티티 변환, 중복 검증, 비밀번호 해시, 저장, 응답 변환
 *
 * 설계 메모
 * 1) Controller는 요청/응답 DTO 바인딩과 HTTP 세부(상태코드 등)만 담당.
 * 2) Service는 순수한 도메인 흐름(검증 → 해시 → 저장)만 담당.
 * 3) Repository는 실제 영속성 접근만 담당.
 *
 * 예외 처리
 * - 지금은 IllegalArgumentException을 던진다.
 *   이후 GlobalExceptionHandler(@RestControllerAdvice)에서
 *   표준 에러 응답으로 변환하도록 개선하면 된다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder; // SecurityConfig에 @Bean 등록됨

    /**
     * 회원가입 유스케이스
     * 1) 입력 정규화(트림 등)
     * 2) username/email 중복 검증
     * 3) 비밀번호 해시(BCrypt)
     * 4) Member 엔티티 생성 및 저장
     * 5) 응답 DTO로 변환하여 반환
     */
    @Transactional
    public SignupDto.SignupResponse signup(SignupDto.SignupRequest req) {
        // 1) 입력 정규화 (공백 제거, 빈 문자열 → null)
        final String username = trim(req.getUsername());
        final String name     = trim(req.getName());
        final String nickname = trimToNull(req.getNickname());
        final String email    = trimToNull(req.getEmail());
        final String rawPw    = req.getPassword(); // 비밀번호는 트림 X (사용자 의도 보존)

        // 2) 중복 검증 (username은 필수, email은 값 있을 때만)
        if (memberRepository.existsByUsername(username)) {
            throw new ConflictException(ErrorCode.DUPLICATE_USERNAME, "이미 사용 중인 username 입니다.");
        }
        if (StringUtils.hasText(email) && memberRepository.existsByEmail(email)) {
            throw new ConflictException(ErrorCode.DUPLICATE_EMAIL, "이미 사용 중인 email 입니다.");
        }

        // 3) 비밀번호 해시(BCrypt)
        final String passwordHash = passwordEncoder.encode(rawPw);

        // 4) 엔티티 조립 (엔티티는 '상태'를 표현, 해시는 평문 대신 저장)
        Member member = Member.builder()
                .username(username)
                .passwordHash(passwordHash)
                .name(name)
                .nickname(nickname)
                .email(email)
                .build();

        // 5) 저장
        Member saved = memberRepository.save(member);

        // 6) 응답 DTO로 변환 (민감정보 제외)
        return SignupDto.SignupResponse.from(saved);
    }

    /* -------------------- 내부 유틸 -------------------- */

    private String trim(String s) {
        return s == null ? null : s.trim();
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}