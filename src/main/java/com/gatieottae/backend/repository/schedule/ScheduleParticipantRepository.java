package com.gatieottae.backend.repository.schedule;

import com.gatieottae.backend.domain.schedule.ScheduleParticipant;
import com.gatieottae.backend.domain.schedule.ScheduleParticipantStatus;
import com.gatieottae.backend.repository.schedule.view.AttendeeSampleView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ScheduleParticipantRepository extends JpaRepository<ScheduleParticipant, Long> {
    @Query("""
    select count(sp) from ScheduleParticipant sp
     where sp.scheduleId = :scheduleId and sp.status = 'GOING'
  """)
    long countGoing(@Param("scheduleId") Long scheduleId);

    boolean existsByScheduleIdAndMemberIdAndStatus(Long scheduleId, Long memberId, ScheduleParticipantStatus status);

    Optional<ScheduleParticipant> findByScheduleIdAndMemberId(Long scheduleId, Long memberId);

    @Query(value = """
    select sp.member_id as memberId,
           coalesce(m.nickname, m.name) as displayName
      from schedule_participant sp
      join member m on m.id = sp.member_id
     where sp.schedule_id = :scheduleId
       and sp.status = 'GOING'
     order by sp.joined_at asc
  """, nativeQuery = true)
    List<AttendeeSampleView> findAttendeeSamples(@Param("scheduleId") Long scheduleId, Pageable pageable);


    void deleteByScheduleId(Long scheduleId);
}