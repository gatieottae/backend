package com.gatieottae.backend.domain.expense;

import jakarta.persistence.*;
import lombok.*;

/**
 * 지출 분담 금액
 * - 동일 expense 내 member 1회만(UNIQUE)
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "expense_share", schema = "gatieottae",
        uniqueConstraints = @UniqueConstraint(name = "uk_expense_share_once", columnNames = {"expense_id","member_id"}))
public class ExpenseShare {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 분담금(원 단위) - NUMERIC→BIGINT 마이그레이션 반영 */
    @Column(name = "share", nullable = false)
    private Long shareAmount;
}