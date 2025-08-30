package com.gatieottae.backend.domain.member;

import com.gatieottae.backend.common.jpa.BaseTimeEntity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "member",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_email", columnNames = "email")
        }
)
public class Member extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255, nullable = false)
    private String email;

    @Column(name = "password_hash", length = 255) // 소셜 전용 계정이면 null 허용
    private String passwordHash;

    @Column(length = 50, nullable = false)
    private String name;

    @Column(length = 50)
    private String nickname;

    @Column(columnDefinition = "text")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private MemberStatus status = MemberStatus.ACTIVE;

    private OffsetDateTime lastLoginAt;

    protected Member() {} // JPA 기본 생성자

    public Member(String email, String name) {
        this.email = email;
        this.name = name;
    }

    // getter/setter 필요시 추가 (롬복 쓰면 @Getter/@Setter)
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public MemberStatus getStatus() { return status; }
}