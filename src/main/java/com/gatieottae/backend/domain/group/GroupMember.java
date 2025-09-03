package com.gatieottae.backend.domain.group;

import com.gatieottae.backend.common.jpa.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * travel_group_member 테이블 매핑 엔티티.
 * - (group_id, member_id) 유니크
 * - role: OWNER/ADMIN/MEMBER (DB CHECK 제약과 일치하도록 Enum 사용)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(
        name = "travel_group_member",
        schema = "gatieottae",
        uniqueConstraints = @UniqueConstraint(name = "uk_travel_group_member", columnNames = {"group_id", "member_id"}),
        indexes = {
                @Index(name = "idx_travel_group_member_group", columnList = "group_id"),
                @Index(name = "idx_travel_group_member_member", columnList = "member_id")
        }
)
public class GroupMember extends BaseTimeEntity {

    public enum Role { OWNER, ADMIN, MEMBER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // BIGSERIAL 매핑
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role;
}