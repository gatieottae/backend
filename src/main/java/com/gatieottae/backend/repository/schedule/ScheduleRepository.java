package com.gatieottae.backend.repository.schedule;

import com.gatieottae.backend.domain.schedule.Schedule;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    /**
     * 캘린더 기간과 겹치는 일정 조회 (그룹 단위)
     * 조건: s.endTime > :from AND s.startTime < :to
     */
    @Query("""
    select s from Schedule s
     where s.groupId = :groupId
       and s.endTime   > :from
       and s.startTime < :to
     order by s.startTime asc, s.id asc
  """)
    List<Schedule> findOverlapping(
            @Param("groupId") Long groupId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    /**
     * 방금 생성된 일정과 겹치는 ID 목록 (중복/겹침 표시용)
     */
    @Query("""
    select s from Schedule s
     where s.groupId = :groupId
       and s.id <> :scheduleId
       and s.endTime   > :start
       and s.startTime < :end
  """)
    List<Schedule> findOverlapsOf(
            @Param("groupId") Long groupId,
            @Param("scheduleId") Long scheduleId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end);
}