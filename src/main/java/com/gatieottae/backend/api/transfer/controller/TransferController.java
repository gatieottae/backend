package com.gatieottae.backend.api.transfer.controller;

import com.gatieottae.backend.api.transfer.dto.*;
import com.gatieottae.backend.security.auth.LoginMember;
import com.gatieottae.backend.service.transfer.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Transfer API", description = "송금 상태 전이/증빙 API")
@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @Operation(summary = "송금 초안 확정(배치 생성)")
    @PostMapping("/commit")
    public ResponseEntity<List<TransferResponseDto>> commit(
            @Valid @RequestBody TransferCommitRequestDto request
    ) {
        return ResponseEntity.ok(transferService.commitDrafts(request));
    }

    @Operation(summary = "보냈어요")
    @PostMapping("/{id}/send")
    public ResponseEntity<TransferResponseDto> send(
            @AuthenticationPrincipal(expression = "id") Long actorMemberId,
            @RequestParam Long groupId,
            @PathVariable Long id,
            @RequestBody(required = false) TransferActionRequestDto body
    ) {
        return ResponseEntity.ok(transferService.markSent(groupId, id, actorMemberId, body));
    }

    @Operation(summary = "받았어요(확인)")
    @PostMapping("/{id}/confirm")
    public ResponseEntity<TransferResponseDto> confirm(
            @AuthenticationPrincipal(expression = "id") Long actorMemberId,
            @RequestParam Long groupId,
            @PathVariable Long id,
            @RequestBody(required = false) TransferActionRequestDto body
    ) {
        return ResponseEntity.ok(transferService.confirm(groupId, id, actorMemberId, body));
    }

    @Operation(summary = "송금 롤백")
    @PostMapping("/{id}/rollback")
    public ResponseEntity<TransferResponseDto> rollback(
            @RequestParam Long groupId,
            @PathVariable Long id,
            @AuthenticationPrincipal LoginMember loginMember,
            @RequestBody(required = false) TransferActionRequestDto body
    ) {
        Long actorMemberId = loginMember.getId();
        boolean isAdmin = loginMember.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return ResponseEntity.ok(
                transferService.rollback(groupId, id, actorMemberId, body, isAdmin)
        );
    }

    @Operation(summary = "증빙 첨부/수정")
    @PostMapping("/{id}/proof")
    public ResponseEntity<TransferResponseDto> attachProof(
            @AuthenticationPrincipal(expression = "id") Long actorMemberId,
            @RequestParam Long groupId,
            @PathVariable Long id,
            @RequestBody TransferProofRequestDto body
    ) {
        return ResponseEntity.ok(transferService.attachProof(groupId, id, actorMemberId, body));
    }

    @Operation(summary = "보채기")
    @PostMapping("/{id}/nudge")
    public ResponseEntity<Void> nudge(
            @AuthenticationPrincipal(expression = "id") Long actorMemberId,
            @RequestParam Long groupId,
            @PathVariable Long id
    ) {
        transferService.nudge(groupId, id, actorMemberId); // 구현은 자유
        return ResponseEntity.ok().build();
    }

    @GetMapping("/groups/{groupId}/transfers/me")
    public List<TransferResponseDto> getMyTransfers(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "id") Long actorMemberId
    ) {
        return transferService.getTransfersForMember(groupId, actorMemberId);
    }
}