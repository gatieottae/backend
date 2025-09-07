package com.gatieottae.backend.domain.schedule;

/**
 * DB ENUM(schedule_participant_status)과 1:1 매핑되는 애플리케이션 enum.
 * - INVITED: 초대됨(기본)
 * - GOING: 참석
 * - NOT_GOING: 불참
 * - TENTATIVE: 미정
 */
public enum ScheduleParticipantStatus {
    INVITED, GOING, NOT_GOING, TENTATIVE
}