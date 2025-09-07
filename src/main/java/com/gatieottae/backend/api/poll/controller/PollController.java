package com.gatieottae.backend.api.poll.controller;

import com.gatieottae.backend.api.poll.dto.PollDto;
import com.gatieottae.backend.service.poll.PollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Poll", description = "투표 API")
@RestController
@RequestMapping("/api/polls")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PollController {

    private final PollService pollService;

    @Operation(
            summary = "투표 생성",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = PollDto.CreateReq.class),
                            examples = {
                                    @ExampleObject(
                                            name = "기본 예시",
                                            value = """
                                            {
                                              "groupId": 2,
                                              "categoryCode": "FOOD",
                                              "title": "저녁식사 어디 갈까요?",
                                              "description": "근처 가성비 위주 제안",
                                              "closesAt": "2025-09-10T18:00:00+09:00",
                                              "options": ["흑돼지", "해물탕", "초밥"]
                                            }
                                            """
                                    )
                            }
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "생성 성공")
            }
    )
    @PostMapping
    public ResponseEntity<PollDto.CreateRes> create(
            @RequestBody @Valid PollDto.CreateReq req,
            @AuthenticationPrincipal(expression = "id") Long memberId
    ) {
        var res = pollService.create(memberId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @Operation(
            summary = "투표하기",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = PollDto.VoteReq.class),
                            examples = @ExampleObject(name="예시", value="{\"optionId\": 101}")
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "204", description = "투표 완료"),
                    @ApiResponse(responseCode = "409", description = "이미 투표함 / 마감됨")
            }
    )
    @PostMapping("/{pollId}/vote")
    public ResponseEntity<Void> vote(
            @PathVariable Long pollId,
            @RequestBody @Valid PollDto.VoteReq req,
            @AuthenticationPrincipal(expression = "id") Long memberId
    ) {
        pollService.vote(pollId, memberId, req.optionId());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "투표 결과 조회",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK")
            }
    )
    @GetMapping("/{pollId}/results")
    public ResponseEntity<PollDto.ResultsRes> results(
            @PathVariable Long pollId,
            @AuthenticationPrincipal(expression = "id") Long memberId
    ) {
        var res = pollService.results(pollId, memberId);
        return ResponseEntity.ok(res);
    }

    @Operation(
            summary = "투표 마감",
            responses = {
                    @ApiResponse(responseCode = "204", description = "마감 완료")
            }
    )
    @PatchMapping("/{pollId}/close")
    public ResponseEntity<Void> close(
            @PathVariable Long pollId,
            @AuthenticationPrincipal(expression = "id") Long memberId
    ) {
        pollService.close(pollId, memberId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "투표 목록 조회")
    @GetMapping
    public ResponseEntity<java.util.List<PollDto.ListItem>> list(
            @RequestParam Long groupId,
            @AuthenticationPrincipal(expression = "id") Long memberId
    ) {
        return ResponseEntity.ok(pollService.list(groupId, memberId));
    }

    @Operation(
            summary = "투표 취소(언투표)",
            responses = {
                    @ApiResponse(responseCode = "204", description = "취소 완료"),
                    @ApiResponse(responseCode = "404", description = "poll not found"),
                    @ApiResponse(responseCode = "409", description = "poll closed")
            }
    )
    @DeleteMapping("/{pollId}/vote")
    public ResponseEntity<Void> unvote(
            @PathVariable Long pollId,
            @AuthenticationPrincipal(expression = "id") Long memberId
    ) {
        pollService.unvote(pollId, memberId);
        return ResponseEntity.noContent().build();
    }
}