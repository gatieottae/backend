package com.gatieottae.backend.repository.schedule;

import com.gatieottae.backend.domain.schedule.ScheduleParticipant;
import com.gatieottae.backend.domain.schedule.ScheduleParticipantStatus;
import com.gatieottae.backend.repository.schedule.view.AttendeeSampleView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScheduleParticipantRepository extends JpaRepository<ScheduleParticipant, Long> {

    /** 참석자 수 (GOING) */
    @Query("""
    select count(sp) from ScheduleParticipant sp
     where sp.scheduleId = :scheduleId and sp.status = 'GOING'
  """)
    long countGoing(@Param("scheduleId") Long scheduleId);

    /** 내가 참석중인지 (isMine) */
    boolean existsByScheduleIdAndMemberIdAndStatus(Long scheduleId, Long memberId, ScheduleParticipantStatus status);

    /**
     * 참석자 샘플 가져오기 (닉네임 우선)
     * - Native: member 조인으로 displayName 구성
     * - Pageable 로 limit 제어 (예: PageRequest.of(0, 2))
     */
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

    /** 일정의 GOING 전체 멤버 id (상세 모달 전체 조회 등에 사용 가능) */
    @Query("""
    select sp.memberId from ScheduleParticipant sp
     where sp.scheduleId = :scheduleId and sp.status = 'GOING'
     order by sp.joinedAt asc
  """)
    List<Long> findAllGoingMemberIds(@Param("scheduleId") Long scheduleId);
}