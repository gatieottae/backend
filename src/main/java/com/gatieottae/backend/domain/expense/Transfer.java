package com.gatieottae.backend.domain.expense;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * 송금(정산) 트랜잭션
 * - settlement → transfer로 전환된 테이블과 매핑
 * - 상태 머신: REQUESTED → SENT → CONFIRMED / ROLLED_BACK
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "transfer", schema = "gatieottae",
        indexes = {
                @Index(name = "idx_transfer_group", columnList = "group_id"),
                @Index(name = "idx_transfer_from_to", columnList = "from_member_id,to_member_id")
        })
public class Transfer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "from_member_id", nullable = false)
    private Long fromMemberId;

    @Column(name = "to_member_id", nullable = false)
    private Long toMemberId;

    /** 송금 금액(원 단위) */
    @Column(nullable = false)
    private Long amount;

    /** DB는 enum 타입, JPA는 문자열로 저장(호환성↑) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;

    @Column(name = "proof_url")
    private String proofUrl;

    private String memo;

    /** DB default now() */
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** DB trigger(now()) */
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}