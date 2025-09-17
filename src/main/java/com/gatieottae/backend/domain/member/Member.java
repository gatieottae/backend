package com.gatieottae.backend.domain.member;

import com.gatieottae.backend.common.jpa.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;


/**
 * 회원 도메인 엔티티
 * - username/password 기반의 로컬 회원가입
 * - email은 선택(Nullable)
 * - createdAt/updatedAt은 BaseTimeEntity에서 자동 세팅
 * ⚠️ 유니크 제약
 * - username: DB/JPA 모두 유니크
 * - email: "값이 있을 때만 유니크"는 DB 파셜 인덱스로 처리 (JPA @Table uniqueConstraints로는 표현 불가)
 */

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 프록시용
@AllArgsConstructor(access = AccessLevel.PRIVATE)  // 빌더 전용
@Builder
@EqualsAndHashCode(of = "id", callSuper = false)
@Entity
@Table(
        name = "member",
        indexes = {
                @Index(name = "ux_member_username", columnList = "username", unique = true),
                // email 파셜 유니크는 migration/DDL(sql)로 처리. 여기서는 일반 인덱스만.
                @Index(name = "ix_member_email", columnList = "email")
        }
)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인 아이디(필수) */
    @Column(length = 50, nullable = false, unique = true)
    private String username;

    /** 비밀번호 해시(필수, 서비스에서 인코딩하여 주입) */
    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    /** 이메일(선택). 값이 있을 때만 유니크 → DB 파셜 인덱스로 보장 */
    @Column(length = 255) // unique=false (기본값)
    private String email;

    /** 실명/표시 이름(필수) */
    @Column(length = 50, nullable = false)
    private String name;

    /** 닉네임(선택) */
    @Column(length = 50)
    private String nickname;

    /** 소셜 로그인 매핑(선택) */
    @Column(length = 20)
    private String oauthProvider;  // 예: "KAKAO"

    @Column(length = 100)
    private String oauthSubject;   // 예: 카카오 user id

    /** 프로필 이미지 URL */
    @Column(name = "profile_image_url", columnDefinition = "text")
    private String profileImageUrl;

    /** 상태값 */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private MemberStatus status = MemberStatus.ACTIVE;

    /** 최근 로그인 시각 */
    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    /* ===================== 신규: 소셜 전용 가입 팩토리 ===================== */

    /**
     * 소셜 전용 계정 생성 팩토리
     * - passwordHash 는 NOT NULL 제약을 만족시키기 위해 플레이스홀더를 저장
     * - name 이 없으면 nickname, 그것도 없으면 "소셜사용자"로 대체
     * - 상태는 기본 ACTIVE, 마지막 로그인 시각은 생성 시점으로 초기화
     */
    public static Member socialSignup(String username,
                                      String email,
                                      String name,
                                      String nickname,
                                      String profileImageUrl,
                                      String provider,
                                      String subject) {

        // NOT NULL 컬럼 보호용 임의 플레이스홀더
        // (운영에선 사용되지 않지만, 스키마 제약을 깨지 않도록 채워둠)
        String placeholderPassword = "{SOCIAL}:" + UUID.randomUUID();

        return Member.builder()
                .username(username)
                .passwordHash(placeholderPassword)
                .email(email)
                .name(Optional.ofNullable(name)
                        .orElse(Optional.ofNullable(nickname).orElse("소셜사용자")))
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .oauthProvider(provider)
                .oauthSubject(subject)
                .status(MemberStatus.ACTIVE)
                .lastLoginAt(OffsetDateTime.now())
                .build();
    }

    /* ===================== 도메인 메서드(최소) ===================== */

    /** 로그인 직후 호출하여 마지막 로그인 시각 기록 */
    public void markLoggedInNow() {
        this.lastLoginAt = OffsetDateTime.now();
    }

    /** 닉네임 변경(간단 검증만, 복잡 검증은 Service/Validator에 위임 권장) */
    public void changeNickname(String newNickname) {
        this.nickname = (newNickname == null || newNickname.isBlank()) ? null : newNickname.trim();
    }

    /** 프로필 이미지 변경 */
    public void changeProfileImage(String imageUrl) {
        this.profileImageUrl = imageUrl;
    }

    /** 상태 변경 (예: 차단/삭제 등) */
    public void changeStatus(MemberStatus newStatus) {
        this.status = newStatus == null ? this.status : newStatus;
    }

    /** 서비스 레이어에서 명시적으로 시각을 주입하고 싶을 때 사용 */
    public void markLastLogin(OffsetDateTime at) {
        this.lastLoginAt = (at == null) ? OffsetDateTime.now() : at;
    }

    public void changeName(String newName) {
        this.name = newName;
    }
}