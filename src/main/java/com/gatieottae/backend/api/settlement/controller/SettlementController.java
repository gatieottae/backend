package com.gatieottae.backend.api.settlement.controller;

import com.gatieottae.backend.api.settlement.dto.SettlementResponseDto;
import com.gatieottae.backend.service.settlement.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Settlement API", description = "정산 계산 스냅샷")
@RestController
@RequestMapping("/api/groups/{groupId}/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @Operation(summary = "그룹 정산(전체 현황)", description = "멤버별 잔액표와 송금 초안 리스트 반환")
    @GetMapping("/overall")
    public ResponseEntity<SettlementResponseDto> overall(@PathVariable Long groupId) {
        return ResponseEntity.ok(settlementService.calculate(groupId));
    }


    @Operation(summary = "나의 정산", description = "현재 사용자 기준으로 받을 돈/보낼 돈만 필터링")
    @GetMapping("/me")
    public ResponseEntity<SettlementResponseDto> me(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "id") Long memberId
    ) {
        return ResponseEntity.ok(settlementService.calculateForMember(groupId, memberId));
    }
}