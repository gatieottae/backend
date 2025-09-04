package com.gatieottae.backend.repository.group;

import com.gatieottae.backend.domain.group.Group;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MyGroupQueryRepository extends CrudRepository<Group, Long> {

    /**
     * START ASC
     * - NULL start_date는 9999-12-31로 간주(= 맨 뒤로)
     * - 정렬: start_date ASC, id DESC (안정 정렬용 타이브레이커)
     * - 커서 비교도 (start_key, id) > (cursor_start_key, cursor_id)
     */
    @Query(value = """
        SELECT g.*
        FROM gatieottae.travel_group g
        JOIN gatieottae.travel_group_member gm ON gm.group_id = g.id
        WHERE gm.member_id = :memberId
          AND (:q IS NULL OR :q = '' OR (g.name ILIKE CONCAT('%', :q, '%') OR g.destination ILIKE CONCAT('%', :q, '%')))
          AND (
             :status IS NULL OR :status = '' OR
             (:status = 'before' AND :today < COALESCE(g.start_date, DATE '9999-12-31')) OR
             (:status = 'during' AND :today BETWEEN COALESCE(g.start_date, :today) AND COALESCE(g.end_date, COALESCE(g.start_date, :today))) OR
             (:status = 'after'  AND :today >  COALESCE(g.end_date, COALESCE(g.start_date, DATE '0001-01-01')))
          )
          AND (
             :cursorId IS NULL OR
             ( (COALESCE(g.start_date, DATE '9999-12-31'), g.id)
               > (COALESCE(:cursorStart, DATE '9999-12-31'), :cursorId) )
          )
        ORDER BY COALESCE(g.start_date, DATE '9999-12-31') ASC, g.id DESC
        LIMIT :size
        """, nativeQuery = true)
    List<Group> findMyGroupsStartAsc(
            @Param("memberId") Long memberId,
            @Param("q") String q,
            @Param("status") String status,
            @Param("today") LocalDate today,
            @Param("cursorStart") LocalDate cursorStart,
            @Param("cursorId") Long cursorId,
            @Param("size") int size
    );

    /**
     * START DESC
     * - NULL start_date는 0001-01-01로 간주(= 맨 앞으로)
     * - 정렬: start_date DESC, id DESC
     * - 커서 비교도 (start_key, id) < (cursor_start_key, cursor_id)
     */
    @Query(value = """
        SELECT g.*
        FROM gatieottae.travel_group g
        JOIN gatieottae.travel_group_member gm ON gm.group_id = g.id
        WHERE gm.member_id = :memberId
          AND (:q IS NULL OR :q = '' OR (g.name ILIKE CONCAT('%', :q, '%') OR g.destination ILIKE CONCAT('%', :q, '%')))
          AND (
             :status IS NULL OR :status = '' OR
             (:status = 'before' AND :today < COALESCE(g.start_date, DATE '9999-12-31')) OR
             (:status = 'during' AND :today BETWEEN COALESCE(g.start_date, :today) AND COALESCE(g.end_date, COALESCE(g.start_date, :today))) OR
             (:status = 'after'  AND :today >  COALESCE(g.end_date, COALESCE(g.start_date, DATE '0001-01-01')))
          )
          AND (
             :cursorId IS NULL OR
             ( (COALESCE(g.start_date, DATE '0001-01-01'), g.id)
               < (COALESCE(:cursorStart, DATE '0001-01-01'), :cursorId) )
          )
        ORDER BY COALESCE(g.start_date, DATE '0001-01-01') DESC, g.id DESC
        LIMIT :size
        """, nativeQuery = true)
    List<Group> findMyGroupsStartDesc(
            @Param("memberId") Long memberId,
            @Param("q") String q,
            @Param("status") String status,
            @Param("today") LocalDate today,
            @Param("cursorStart") LocalDate cursorStart,
            @Param("cursorId") Long cursorId,
            @Param("size") int size
    );

    /**
     * TITLE ASC
     * - 정렬: LOWER(name) ASC, id DESC
     * - 커서 비교도 (LOWER(name), id) > (LOWER(cursorTitle), cursorId)
     */
    @Query(value = """
        SELECT g.*
        FROM gatieottae.travel_group g
        JOIN gatieottae.travel_group_member gm ON gm.group_id = g.id
        WHERE gm.member_id = :memberId
          AND (:q IS NULL OR :q = '' OR (g.name ILIKE CONCAT('%', :q, '%') OR g.destination ILIKE CONCAT('%', :q, '%')))
          AND (
             :status IS NULL OR :status = '' OR
             (:status = 'before' AND :today < COALESCE(g.start_date, DATE '9999-12-31')) OR
             (:status = 'during' AND :today BETWEEN COALESCE(g.start_date, :today) AND COALESCE(g.end_date, COALESCE(g.start_date, :today))) OR
             (:status = 'after'  AND :today >  COALESCE(g.end_date, COALESCE(g.start_date, DATE '0001-01-01')))
          )
          AND (
             :cursorId IS NULL OR
             ( (LOWER(g.name), g.id) > (LOWER(:cursorTitle), :cursorId) )
          )
        ORDER BY LOWER(g.name) ASC, g.id DESC
        LIMIT :size
        """, nativeQuery = true)
    List<Group> findMyGroupsTitleAsc(
            @Param("memberId") Long memberId,
            @Param("q") String q,
            @Param("status") String status,
            @Param("today") LocalDate today,
            @Param("cursorTitle") String cursorTitle,
            @Param("cursorId") Long cursorId,
            @Param("size") int size
    );
}