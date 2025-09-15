package com.gatieottae.backend.api.settlement.controller;

import com.gatieottae.backend.api.settlement.dto.SettlementResponseDto;
import com.gatieottae.backend.service.settlement.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Settlement API", description = "정산 계산 스냅샷")
@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @Operation(summary = "그룹 정산 계산", description = "멤버별 잔액표와 송금 초안 리스트를 반환한다.")
    @GetMapping("/{groupId}")
    public ResponseEntity<SettlementResponseDto> calculate(@PathVariable Long groupId) {
        return ResponseEntity.ok(settlementService.calculate(groupId));
    }
}