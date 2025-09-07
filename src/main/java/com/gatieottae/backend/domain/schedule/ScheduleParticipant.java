package com.gatieottae.backend.domain.schedule;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * 일정 참가자 엔티티
 * - UNIQUE (schedule_id, member_id) 중복 참가 방지
 * - 상태 컬럼은 PostgreSQL ENUM(schedule_participant_status)와 매핑
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
        name = "schedule_participant",
        uniqueConstraints = @UniqueConstraint(name = "uq_schedule_member", columnNames = {"schedule_id","member_id"}),
        indexes = @Index(name = "idx_sp_schedule_status", columnList = "schedule_id,status,joined_at")
)
public class ScheduleParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /**
     * Hibernate 6: NAMED_ENUM + columnDefinition 으로 DB ENUM과 연결
     * 주의) Boot 3/Hibernate 6 환경이 아니면 커스텀 타입 필요
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "schedule_participant_status")
    private ScheduleParticipantStatus status;

    /** DB default now() → 읽기 전용 매핑 */
    @Column(name = "joined_at", insertable = false, updatable = false)
    private OffsetDateTime joinedAt;
}