package com.gatieottae.backend.domain.group;

import com.gatieottae.backend.common.jpa.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

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

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role;

    // 정적 팩토리
    public static GroupMember create(Group group, Long memberId, Role role) {
        return GroupMember.builder()
                .groupId(group.getId())
                .memberId(memberId)
                .role(role)
                .build();
    }
}