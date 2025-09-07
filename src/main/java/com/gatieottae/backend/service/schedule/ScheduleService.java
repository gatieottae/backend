// src/main/java/com/gatieottae/backend/service/schedule/ScheduleService.java
package com.gatieottae.backend.service.schedule;

import com.gatieottae.backend.api.schedule.dto.ScheduleDto;
import com.gatieottae.backend.domain.schedule.Schedule;
import com.gatieottae.backend.domain.schedule.ScheduleParticipant;
import com.gatieottae.backend.domain.schedule.ScheduleParticipantStatus;
import com.gatieottae.backend.repository.schedule.ScheduleParticipantRepository;
import com.gatieottae.backend.repository.schedule.ScheduleRepository;
import com.gatieottae.backend.repository.schedule.view.AttendeeSampleView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepo;
    private final ScheduleParticipantRepository spRepo;

    /**
     * 일정 생성
     * - end > start 검증 실패 시 400
     * - 동일 그룹/제목/시간 완전 중복은 DB 유니크로 차단
     * - 생성자는 자동 GOING
     * - 겹치는 일정 ID 목록을 반환하여 UI가 즉시 강조 가능
     */
    @Transactional
    public ScheduleDto.CreateRes create(Long groupId, Long memberId, ScheduleDto.CreateReq req) {
        // 1) 입력 검증 (클린 에러 메시지)
        if (!StringUtils.hasText(req.title())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required");
        }
        if (req.startTime() == null || req.endTime() == null || !req.startTime().isBefore(req.endTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endTime must be after startTime");
        }

        // (선택) 그룹 멤버십/권한 검증은 여기서 수행할 수 있음
        // validateGroupMember(groupId, memberId);

        // 2) 저장
        Schedule s = Schedule.builder()
                .groupId(groupId)
                .title(req.title().trim())
                .description(req.description())
                .location(req.location())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .createdBy(memberId)
                .build();
        scheduleRepo.save(s);

        // 3) 생성자 자동 참석(GOING) — 멱등을 고려해 존재하면 변경, 없으면 추가
        upsertAttendance(s.getId(), memberId, ScheduleParticipantStatus.GOING);

        // 4) 겹침 목록 수집
        List<Long> overlappedIds = scheduleRepo.findOverlapsOf(groupId, s.getId(), req.startTime(), req.endTime())
                .stream().map(Schedule::getId).toList();

        return new ScheduleDto.CreateRes(s.getId(), overlappedIds);
    }

    /**
     * 기간 조회 (달력)
     * - 조건: end > :from AND start < :to
     * - 참석자 요약: count + sample(최대 2명, joined_at asc) + isMine
     */
    @Transactional(readOnly = true)
    public List<ScheduleDto.Item> list(Long groupId, OffsetDateTime from, OffsetDateTime to, Long me, boolean markOverlap) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid range: from must be before to");
        }

        var schedules = scheduleRepo.findOverlapping(groupId, from, to);
        var result = new ArrayList<ScheduleDto.Item>(schedules.size());

        for (Schedule s : schedules) {
            long going = spRepo.countGoing(s.getId());

            // sample 2명 (joined_at asc)
            var samples = spRepo.findAttendeeSamples(s.getId(), PageRequest.of(0, 2));
            var sampleDtos = samples.stream()
                    .map(v -> new ScheduleDto.Item.Member(v.getMemberId(), v.getDisplayName()))
                    .toList();

            boolean isMine = spRepo.existsByScheduleIdAndMemberIdAndStatus(s.getId(), me, ScheduleParticipantStatus.GOING);
            boolean hasMore = going > sampleDtos.size();

            var attending = new ScheduleDto.Item.Attending(going, sampleDtos, hasMore, isMine);

            result.add(new ScheduleDto.Item(
                    s.getId(), s.getTitle(), s.getLocation(),
                    s.getStartTime(), s.getEndTime(),
                    attending,
                    markOverlap // 프론트에서 겹침 스타일링 여부에 활용(옵션)
            ));
        }

        return result;
    }

    /**
     * 참여 상태 설정 (멱등 Upsert)
     * - 허용: GOING / NOT_GOING / TENTATIVE
     * - 잘못된 status는 400
     */
    @Transactional
    public void setAttendance(Long scheduleId, Long memberId, String statusText) {
        ScheduleParticipantStatus status = parseStatus(statusText);

        // (선택) 일정 존재/그룹 멤버십 검증
        if (!scheduleRepo.existsById(scheduleId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "schedule not found");
        }
        // validateGroupMemberBySchedule(scheduleId, memberId);

        upsertAttendance(scheduleId, memberId, status);
    }

    // ==========================
    // 내부 유틸
    // ==========================

    private void upsertAttendance(Long scheduleId, Long memberId, ScheduleParticipantStatus status) {
        // exists → update, not exists → insert (간단·안전)
        var opt = spRepo.findAll().stream() // NOTE: 실무에서는 PK/복합키로 조회하는 메서드를 레포에 추가하세요.
                .filter(x -> x.getScheduleId().equals(scheduleId) && x.getMemberId().equals(memberId))
                .findFirst();

        ScheduleParticipant sp = opt.orElseGet(() ->
                ScheduleParticipant.builder()
                        .scheduleId(scheduleId)
                        .memberId(memberId)
                        .status(status)
                        .build()
        );
        sp.setStatus(status);
        spRepo.save(sp);
    }

    private ScheduleParticipantStatus parseStatus(String statusText) {
        if (statusText == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        try {
            // 입력은 대문자 기준 (프론트에서 enum값 그대로 보내도록 합의)
            return ScheduleParticipantStatus.valueOf(statusText);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid status: " + statusText);
        }
    }

    // (선택) 그룹 멤버십 검증 — Security/JWT 붙인 후 활성화
    @SuppressWarnings("unused")
    private void validateGroupMember(Long groupId, Long memberId) {
        // TODO: travel_group_member 존재 여부 확인 후, 없으면 403
        // throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not a member of the group");
    }

    @SuppressWarnings("unused")
    private void validateGroupMemberBySchedule(Long scheduleId, Long memberId) {
        // TODO: schedule → groupId 로딩 후 validateGroupMember(groupId, memberId)
    }
}