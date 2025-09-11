package com.gatieottae.backend.service.auth;

import com.gatieottae.backend.api.auth.dto.LoginDto;
import com.gatieottae.backend.api.auth.dto.SignupDto;
import com.gatieottae.backend.common.exception.ConflictException;
import com.gatieottae.backend.common.exception.ErrorCode;
import com.gatieottae.backend.domain.member.Member;
import com.gatieottae.backend.repository.member.MemberRepository;
import com.gatieottae.backend.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.gatieottae.backend.common.exception.BadRequestException;
import com.gatieottae.backend.domain.member.MemberStatus;

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
    private final JwtTokenProvider jwtTokenProvider; // JWT 발급/검증 유틸

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

    /**
     * 로그인 유스케이스
     * 1) username 정규화/조회
     * 2) 계정 상태 확인 (ACTIVE만 허용)
     * 3) 비밀번호(BCrypt) 검증
     * 4) access/refresh 발급 후 응답 DTO 반환
     *
     * 보안상 이유로 "아이디/비번 중 무엇이 틀렸는지"는 구체적으로 말하지 않는다.
     */
    public LoginDto.LoginResponse login(LoginDto.LoginRequest req) {
        final String username = trim(req.getUsername());
        final String rawPw    = req.getPassword();

        // 1) 사용자 조회 (존재하지 않으면 동일한 에러로 응답)
        Member m = memberRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException(
                        ErrorCode.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."));

        // 2) 계정 상태 체크 (비활성/차단 등)
        if (m.getStatus() != MemberStatus.ACTIVE) {
            // 상태 이슈는 403으로 내려가도록 FORBIDDEN 코드 사용
            throw new ConflictException(ErrorCode.FORBIDDEN, "비활성화된 사용자입니다.");
        }

        // 3) 비밀번호 검증(BCrypt)
        if (!passwordEncoder.matches(rawPw, m.getPasswordHash())) {
            throw new BadRequestException(
                    ErrorCode.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        // 4) JWT 발급
        String accessToken  = jwtTokenProvider.generateAccessToken(m.getUsername(), m.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(m.getUsername(), m.getId());

        return LoginDto.LoginResponse.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * 토큰 재발급 유스케이스
     * 1) refreshToken 유효성 검증 (시그니처/만료 확인)
     * 2) refreshToken에서 subject(username)와 memberId 추출
     * 3) DB에서 사용자 조회 → 상태 확인
     * 4) 새 accessToken 발급 (refreshToken은 그대로 반환)
     */
    public LoginDto.LoginResponse refresh(String refreshToken) {
        // 1) refreshToken 유효성 검증
        if (!jwtTokenProvider.validate(refreshToken)) {
            throw new BadRequestException(ErrorCode.UNAUTHORIZED, "유효하지 않은 refreshToken 입니다.");
        }

        // 2) refreshToken에서 username, memberId 추출
        String username = jwtTokenProvider.getUsername(refreshToken);
        Long memberId   = jwtTokenProvider.getMemberId(refreshToken);

        // 3) 사용자 조회 + 상태 확인
        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new BadRequestException(ErrorCode.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));

        if (!m.getUsername().equals(username)) {
            // 토큰의 username과 DB 불일치 → 위조 가능성
            throw new BadRequestException(ErrorCode.UNAUTHORIZED, "토큰 정보가 올바르지 않습니다.");
        }

        if (m.getStatus() != MemberStatus.ACTIVE) {
            throw new ConflictException(ErrorCode.FORBIDDEN, "비활성화된 사용자입니다.");
        }

        // 4) 새 Access Token 발급
        String newAccessToken = jwtTokenProvider.generateAccessToken(m.getUsername(), m.getId());

        return LoginDto.LoginResponse.builder()
                .tokenType("Bearer")
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // refreshToken은 그대로 반환
                .build();
    }

    public com.gatieottae.backend.domain.member.Member getMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new com.gatieottae.backend.common.exception.BadRequestException(com.gatieottae.backend.common.exception.ErrorCode.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));
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