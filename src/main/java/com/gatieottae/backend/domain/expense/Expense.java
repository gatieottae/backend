package com.gatieottae.backend.domain.expense;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 지출(Expense)
 * - 금액 Long(원 단위)
 * - shares 합계 = amount (서비스 레벨 검증)
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "expense", schema = "gatieottae",
        indexes = {
                @Index(name = "idx_expense_group_paidat", columnList = "group_id, paid_at")
        })
public class Expense {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(nullable = false, length = 128)
    private String title;

    /** 총 지출 금액(원 단위) - NUMERIC→BIGINT 마이그레이션 반영 */
    @Column(nullable = false)
    private Long amount;

    /** 지불자 member.id */
    @Column(name = "paid_by")
    private Long paidBy;

    @Column(name = "paid_at", nullable = false)
    private OffsetDateTime paidAt;

    /** DB default now(), trigger 로 관리 */
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** DB trigger 로 now() 업데이트 */
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExpenseShare> shares = new ArrayList<>();

    /** 연관관계 편의 메서드 */
    public void addShare(ExpenseShare share) {
        share.setExpense(this);
        this.shares.add(share);
    }
}