// src/main/java/com/gatieottae/backend/api/schedule/controller/ScheduleController.java
package com.gatieottae.backend.api.schedule.controller;

import com.gatieottae.backend.api.schedule.dto.ScheduleDto;
import com.gatieottae.backend.service.schedule.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups/{groupId}/schedules")
@Tag(name = "Schedule", description = "그룹 일정 API")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @Operation(summary = "일정 생성", description = "겹침 허용. 생성자 자동 GOING. 겹치는 일정 ID를 응답.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleDto.CreateRes create(
            @PathVariable Long groupId,
            @Valid @RequestBody ScheduleDto.CreateReq req,
            @RequestHeader("X-Member-Id") Long me
    ) {
        return scheduleService.create(groupId, me, req);
    }

    @Operation(summary = "일정 기간 조회", description = "조건: end > from && start < to")
    @GetMapping
    public List<ScheduleDto.Item> list(
            @PathVariable Long groupId,
            @Parameter(description = "포함 시작(ISO-8601)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @Parameter(description = "제외 끝(ISO-8601)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestHeader("X-Member-Id") Long me
    ) {
        return scheduleService.list(groupId, from, to, me, true);
    }
}