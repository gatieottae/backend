package com.gatieottae.backend.domain.schedule;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

/**
 * 일정 엔티티
 * - DB: schedule (TIMESTAMPTZ 매핑 → OffsetDateTime)
 * - 무결성: end_time > start_time (DB CHECK로 보장)
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
        name = "schedule",
        indexes = {
                @Index(name = "idx_schedule_group_time", columnList = "group_id,start_time,end_time")
        }
        // UNIQUE (group_id, title, start_time, COALESCE(end_time, start_time)) 는 DB 인덱스로 관리
)
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 255)
    private String location;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time")
    private OffsetDateTime endTime;

    @Column(name = "created_by")
    private Long createdBy;

    /** DB default now() & trigger로 관리 → 읽기 전용 매핑 */
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** DB trigger set_updated_at()로 자동 갱신 → 읽기 전용 매핑 */
    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}