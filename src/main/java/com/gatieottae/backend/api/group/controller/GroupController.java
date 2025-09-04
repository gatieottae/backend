package com.gatieottae.backend.api.group.controller;

import com.gatieottae.backend.api.group.dto.GroupJoinRequestDto;
import com.gatieottae.backend.api.group.dto.GroupRequestDto;
import com.gatieottae.backend.api.group.dto.GroupResponseDto;
import com.gatieottae.backend.domain.group.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @Operation(summary = "그룹 생성", description = "새 그룹을 만들고 생성자를 OWNER로 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "409", description = "중복 그룹명")
    })
    @PostMapping
    public ResponseEntity<GroupResponseDto> createGroup(
            @Valid @RequestBody GroupRequestDto request,
            // ✅ JwtAuthenticationFilter가 넣어준 LoginMember의 id를 바로 꺼내씀
            @AuthenticationPrincipal(expression = "id") Long userId
    ) {
        GroupResponseDto response = groupService.createGroup(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "초대코드로 참여", description = "초대 코드를 입력해 그룹에 참여합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "참여 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "코드 불일치/만료"),
            @ApiResponse(responseCode = "409", description = "이미 멤버")
    })
    @PostMapping("/join/code")
    public ResponseEntity<GroupResponseDto> joinByCode(
            @Valid @RequestBody GroupJoinRequestDto request,
            @AuthenticationPrincipal(expression = "id") Long userId
    ) {
        return ResponseEntity.ok(groupService.joinByCode(request.getCode(), userId));
    }

    @Operation(summary = "그룹 수정", description = "OWNER만 그룹 정보를 수정할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "그룹 없음")
    })
    @PutMapping("/{id}")
    public ResponseEntity<GroupResponseDto> updateGroup(
            @PathVariable Long id,
            @Valid @RequestBody GroupRequestDto request,
            @AuthenticationPrincipal(expression = "id") Long userId
    ) {
        GroupResponseDto response = groupService.updateGroup(id, userId, request);
        return ResponseEntity.ok(response);
    }
}