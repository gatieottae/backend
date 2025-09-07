package com.gatieottae.backend.repository.schedule;

import com.gatieottae.backend.domain.schedule.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    @Query("""
    select s from Schedule s
     where s.groupId = :groupId
       and s.endTime   > :from
       and s.startTime < :to
     order by s.startTime asc, s.id asc
  """)
    List<Schedule> findOverlapping(@Param("groupId") Long groupId,
                                   @Param("from") OffsetDateTime from,
                                   @Param("to") OffsetDateTime to);

    @Query("""
    select s from Schedule s
     where s.groupId = :groupId
       and s.id <> :scheduleId
       and s.endTime   > :start
       and s.startTime < :end
  """)
    List<Schedule> findOverlapsOf(@Param("groupId") Long groupId,
                                  @Param("scheduleId") Long scheduleId,
                                  @Param("start") OffsetDateTime start,
                                  @Param("end") OffsetDateTime end);

    Optional<Schedule> findByIdAndGroupId(Long id, Long groupId);
}