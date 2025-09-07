package com.gatieottae.backend.api.schedule.controller;

import com.gatieottae.backend.service.schedule.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules/{scheduleId}")
public class ScheduleAttendanceController {

    private final ScheduleService scheduleService;

    public record PutReq(@NotBlank String status) {}

    @Operation(summary = "참여 상태 변경", description = "허용: GOING / NOT_GOING / TENTATIVE")
    @PutMapping("/attendance")
    public ResponseEntity<Void> setAttendance(
            @PathVariable Long scheduleId,
            @Valid @RequestBody PutReq body,
            @AuthenticationPrincipal(expression = "id") Long memberId
    ) {
        scheduleService.setAttendance(scheduleId, memberId, body.status());
        return ResponseEntity.noContent().build();
    }
}