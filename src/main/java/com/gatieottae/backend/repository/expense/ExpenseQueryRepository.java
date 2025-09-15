package com.gatieottae.backend.repository.expense;

import com.gatieottae.backend.domain.expense.Expense;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@Repository
public class ExpenseQueryRepository {

    @PersistenceContext
    private EntityManager em;

    /** 그룹 지출과 share를 fetch join으로 한번에 */
    public List<Expense> findExpensesWithSharesByGroupId(Long groupId) {
        return em.createQuery("""
                select distinct e
                from Expense e
                left join fetch e.shares s
                where e.groupId = :gid
                order by e.paidAt desc
                """, Expense.class)
                .setParameter("gid", groupId)
                .getResultList();
    }
}