// src/main/java/com/gatieottae/backend/api/schedule/controller/ScheduleController.java
package com.gatieottae.backend.api.schedule.controller;

import com.gatieottae.backend.api.schedule.dto.ScheduleDto;
import com.gatieottae.backend.service.schedule.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Tag(name = "Schedule", description = "그룹 일정 API")
@RestController
@RequestMapping("/api/groups/{groupId}/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @Operation(
            summary = "일정 조회(하루 또는 기간)",
            description = """
            선택한 날짜의 하루 또는 임의 기간의 일정을 조회합니다.

            📌 사용법
            - **하루 조회**: `date=2025-09-07`
            - **기간 조회**: `from=2025-09-07T00:00:00+09:00&to=2025-09-08T00:00:00+09:00`
            
            규칙: `end > from` AND `start < to` (겹치는 일정 포함)
            """,
            responses = { @ApiResponse(responseCode = "200", description = "OK") }
    )
    @GetMapping
    public ResponseEntity<List<ScheduleDto.Item>> list(
            @Parameter(description = "그룹 ID", example = "2")
            @PathVariable Long groupId,

            @Parameter(description = "YYYY-MM-DD (예: 2025-09-07)", example = "2025-09-07")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @Parameter(description = "포함 시작(ISO-8601)", example = "2025-09-07T00:00:00+09:00")
            @RequestParam(required = false)
            OffsetDateTime from,

            @Parameter(description = "제외 끝(ISO-8601)", example = "2025-09-08T00:00:00+09:00")
            @RequestParam(required = false)
            OffsetDateTime to,

            // isMine 계산 등에 활용
            @AuthenticationPrincipal(expression = "id") Long memberId
    ) {
        // 1) date만 온 경우 — 하루 범위로 변환 (오프셋이 없는 LocalDate → 시스템 기본 오프셋 사용)
        if (date != null && (from == null && to == null)) {
            // 서버 표준 오프셋을 쓰거나, Asia/Seoul 기준 오프셋을 명시적으로 계산해도 됩니다.
            ZoneOffset systemOffset = OffsetDateTime.now().getOffset();
            from = date.atStartOfDay().atOffset(systemOffset);
            to   = date.plusDays(1).atStartOfDay().atOffset(systemOffset);
        }

        // 2) 유효성 점검
        if (from == null || to == null) {
            return ResponseEntity.badRequest().build();
        }
        if (!from.isBefore(to)) {
            return ResponseEntity.badRequest().build();
        }

        var items = scheduleService.list(groupId, from, to, memberId, true);
        return ResponseEntity.ok(items);
    }

    @Operation(
            summary = "일정 생성",
            description = """
            새로운 일정을 생성합니다.

            ✅ 유효성
            - `startTime` < `endTime`
            - 동일 그룹/제목/시간 **완전 중복** 금지 (DB 유니크)

            성공 시 생성된 ID와 겹치는 일정 ID 목록을 반환합니다.
            """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ScheduleDto.CreateReq.class),
                            examples = @ExampleObject(
                                    name = "성산일출봉 일출",
                                    value = """
                    {
                      "title": "성산일출봉 일출",
                      "description": "일출 포인트로 이동",
                      "location": "성산일출봉 주차장",
                      "startTime": "2025-09-07T05:40:00+09:00",
                      "endTime":   "2025-09-07T07:30:00+09:00"
                    }
                    """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "생성 성공"),
                    @ApiResponse(responseCode = "400", description = "유효성 실패"),
                    @ApiResponse(responseCode = "401", description = "인증 필요"),
                    @ApiResponse(responseCode = "409", description = "중복/제약 위반")
            }
    )
    @PostMapping
    public ResponseEntity<ScheduleDto.CreateRes> create(
            @Parameter(description = "그룹 ID", example = "2")
            @PathVariable Long groupId,
            @RequestBody ScheduleDto.CreateReq req,
            // 생성자 ID(참가자 자동 GOING에 필요)
            @AuthenticationPrincipal(expression = "id") Long memberId
    ) {
        var res = scheduleService.create(groupId, memberId, req);
        return ResponseEntity.status(201).body(res);
    }

    @Operation(
            summary = "일정 수정",
            description = """
            일정 정보를 수정합니다. 
            - 수정 가능 필드: `title`, `description`, `location`, `startTime`, `endTime`
            - 일부만 보낼 경우 해당 필드만 갱신됩니다.
            - 시간 변경 시 반드시 `startTime < endTime` 이어야 합니다.
            """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ScheduleDto.UpdateReq.class),
                            examples = {
                                    @ExampleObject(
                                            name = "시간만 수정",
                                            value = """
                                        {
                                          "startTime": "2025-09-07T06:00:00+09:00",
                                          "endTime":   "2025-09-07T08:00:00+09:00"
                                        }
                                        """
                                    ),
                                    @ExampleObject(
                                            name = "제목 + 설명 수정",
                                            value = """
                                        {
                                          "title": "제주 오름 산책",
                                          "description": "오름 정상에서 일출 감상"
                                        }
                                        """
                                    ),
                                    @ExampleObject(
                                            name = "전체 수정",
                                            value = """
                                        {
                                          "title": "성산일출봉 일출",
                                          "description": "일출 포인트로 이동",
                                          "location": "성산일출봉 주차장",
                                          "startTime": "2025-09-07T05:40:00+09:00",
                                          "endTime":   "2025-09-07T07:30:00+09:00"
                                        }
                                        """
                                    )
                            }
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "수정 성공"),
                    @ApiResponse(responseCode = "400", description = "검증 실패"),
                    @ApiResponse(responseCode = "401", description = "인증 필요"),
                    @ApiResponse(responseCode = "403", description = "권한 없음"),
                    @ApiResponse(responseCode = "404", description = "일정 없음")
            }
    )
    @PutMapping("/{scheduleId}")
    public ResponseEntity<ScheduleDto.CreateRes> update(
            @PathVariable Long groupId,
            @PathVariable Long scheduleId,
            @RequestBody ScheduleDto.UpdateReq req,
            @AuthenticationPrincipal(expression = "id") Long memberId
    ) {
        var res = scheduleService.update(groupId, scheduleId, memberId, req);
        return ResponseEntity.ok(res);
    }

    @Operation(
            summary = "일정 삭제",
            description = "지정한 일정을 삭제합니다. OWNER 또는 작성자만 삭제할 수 있습니다.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "삭제 성공"),
                    @ApiResponse(responseCode = "401", description = "인증 필요"),
                    @ApiResponse(responseCode = "403", description = "권한 없음"),
                    @ApiResponse(responseCode = "404", description = "일정 없음")
            }
    )
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long groupId,
            @PathVariable Long scheduleId,
            @AuthenticationPrincipal(expression = "id") Long memberId
    ) {
        scheduleService.delete(groupId, scheduleId, memberId);
        return ResponseEntity.noContent().build();
    }
}