package com.gatieottae.backend.api.schedule.controller;

import com.gatieottae.backend.api.schedule.dto.ScheduleDto;
import com.gatieottae.backend.service.schedule.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules/{scheduleId}")
public class ScheduleAttendanceController {

    private final ScheduleService scheduleService;

    @Operation(summary = "참여 상태 변경", description = "GOING/NOT_GOING/TENTATIVE")
    @PutMapping("/attendance")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setAttendance(
            @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleDto.AttendanceReq req,
            @RequestHeader("X-Member-Id") Long me
    ) {
        scheduleService.setAttendance(scheduleId, me, req.status());
    }
}