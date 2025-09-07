package com.gatieottae.backend.api.schedule.dto;

import java.time.OffsetDateTime;
import java.util.List;

public class ScheduleDto {
    public record CreateReq(String title, String description, String location,
                            OffsetDateTime startTime, OffsetDateTime endTime) {}
    public record CreateRes(Long id, List<Long> overlappedIds) {}

    public record Item(Long id, String title, String location,
                       OffsetDateTime startTime, OffsetDateTime endTime,
                       Attending attending, boolean overlap) {
        public record Attending(long count, List<Member> sample, boolean hasMore, boolean isMine) {}
        public record Member(Long memberId, String displayName) {}
    }

    public record AttendanceReq(String status) {} // GOING | NOT_GOING | TENTATIVE
}