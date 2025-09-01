package com.gatieottae.backend.service.auth;

import com.gatieottae.backend.api.auth.dto.LoginDto;
import com.gatieottae.backend.common.exception.BadRequestException;
import com.gatieottae.backend.common.exception.ConflictException;
import com.gatieottae.backend.domain.member.Member;
import com.gatieottae.backend.domain.member.MemberStatus;
import com.gatieottae.backend.repository.member.MemberRepository;
import com.gatieottae.backend.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceRefreshTest {

    @Mock private MemberRepository memberRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private Member activeMember;

    @BeforeEach
    void setUp() {
        activeMember = Member.builder()
                .username("alice")
                .passwordHash("$2a$10$dummy") // 사용 안함
                .name("앨리스")
                .nickname("앨리")
                .email("alice@example.com")
                .status(MemberStatus.ACTIVE)   // ✅ 활성 사용자로 명시
                .build();
        // 테스트에서 id가 필요하므로 리플렉션으로 세팅
        setId(activeMember, 1L);
    }

    private static void setId(Object target, Long idVal) {
        try {
            Field f = target.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(target, idVal);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    class Success {

        @Test
        @DisplayName("유효한 refreshToken이면 새 accessToken을 발급하고 refresh는 그대로 반환한다")
        void refresh_success() {
            String refresh = "valid-refresh";
            when(jwtTokenProvider.validate(refresh)).thenReturn(true);
            when(jwtTokenProvider.getUsername(refresh)).thenReturn("alice");
            when(jwtTokenProvider.getMemberId(refresh)).thenReturn(1L);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(activeMember));
            when(jwtTokenProvider.generateAccessToken("alice", 1L)).thenReturn("new-access");

            LoginDto.LoginResponse res = authService.refresh(refresh);

            assertThat(res.getTokenType()).isEqualTo("Bearer");
            assertThat(res.getAccessToken()).isEqualTo("new-access");
            assertThat(res.getRefreshToken()).isEqualTo(refresh);

            verify(jwtTokenProvider).validate(refresh);
            verify(jwtTokenProvider).getUsername(refresh);
            verify(jwtTokenProvider).getMemberId(refresh);
            verify(jwtTokenProvider).generateAccessToken("alice", 1L);
        }
    }

    @Nested
    class Failures {

        @Test
        @DisplayName("refreshToken 무효/만료 → BadRequestException(401 매핑)")
        void refresh_invalid_token() {
            String refresh = "invalid";
            when(jwtTokenProvider.validate(refresh)).thenReturn(false);

            assertThatThrownBy(() -> authService.refresh(refresh))
                    .isInstanceOf(BadRequestException.class);

            verify(jwtTokenProvider).validate(refresh);
            verify(jwtTokenProvider, never()).getUsername(ArgumentMatchers.anyString());
        }

        @Test
        @DisplayName("토큰 username과 DB username 불일치 → BadRequestException")
        void refresh_username_mismatch() {
            String refresh = "valid";
            when(jwtTokenProvider.validate(refresh)).thenReturn(true);
            when(jwtTokenProvider.getUsername(refresh)).thenReturn("bob"); // 토큰은 bob
            when(jwtTokenProvider.getMemberId(refresh)).thenReturn(1L);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(activeMember)); // DB는 alice

            assertThatThrownBy(() -> authService.refresh(refresh))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("사용자 BLOCKED 상태 → ConflictException(403 매핑)")
        void refresh_blocked_member() {
            String refresh = "valid";
            Member blocked = Member.builder()
                    .username("alice")
                    .passwordHash("$2a")
                    .name("앨리스")
                    .status(MemberStatus.BLOCKED)
                    .build();
            setId(blocked, 1L);

            when(jwtTokenProvider.validate(refresh)).thenReturn(true);
            when(jwtTokenProvider.getUsername(refresh)).thenReturn("alice");
            when(jwtTokenProvider.getMemberId(refresh)).thenReturn(1L);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(blocked));

            assertThatThrownBy(() -> authService.refresh(refresh))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("사용자 DELETED 상태 → ConflictException(403 매핑)")
        void refresh_deleted_member() {
            String refresh = "valid";
            Member deleted = Member.builder()
                    .username("alice")
                    .passwordHash("$2a")
                    .name("앨리스")
                    .status(MemberStatus.DELETED)
                    .build();
            setId(deleted, 1L);

            when(jwtTokenProvider.validate(refresh)).thenReturn(true);
            when(jwtTokenProvider.getUsername(refresh)).thenReturn("alice");
            when(jwtTokenProvider.getMemberId(refresh)).thenReturn(1L);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(deleted));

            assertThatThrownBy(() -> authService.refresh(refresh))
                    .isInstanceOf(ConflictException.class);
        }
    }
}