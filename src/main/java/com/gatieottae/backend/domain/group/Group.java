package com.gatieottae.backend.domain.group;

import com.gatieottae.backend.common.jpa.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * travel_group 테이블 매핑 엔티티.
 * - 동일 owner 내 name 유니크 (DB Unique 제약과 일치)
 * - 단일 초대코드(invite_code) + 만료/회전 시각
 */

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true) // 테스트에서 toBuilder() 쓸 수 있도록
@Entity
@Table(
        name = "travel_group",
        schema = "gatieottae",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_travel_group_owner_name", columnNames = {"owner_id","name"})
        },
        indexes = {
                @Index(name = "ux_travel_group_invite_code", columnList = "invite_code", unique = true)
        }
)
public class Group extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)   // 30자 제한 반영
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    // 여행 정보
    @Column(length = 100)
    private String destination;

    private LocalDate startDate;
    private LocalDate endDate;

    // 초대코드(단일, 만료 없음)
    @Column(name = "invite_code", length = 12, unique = true)
    private String inviteCode;
}