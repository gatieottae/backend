package com.gatieottae.backend.api.expense.controller;

import com.gatieottae.backend.api.expense.dto.ExpenseRequestDto;
import com.gatieottae.backend.api.expense.dto.ExpenseResponseDto;
import com.gatieottae.backend.service.expense.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Expense API", description = "지출 CRUD API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    /* =======================
     * RESTful (권장) 경로 – 프론트와 일치
     * /api/groups/{groupId}/expenses[/{id}]
     * ======================= */

    @Operation(summary = "그룹별 지출 목록 조회")
    @GetMapping("/groups/{groupId}/expenses")
    public ResponseEntity<List<ExpenseResponseDto>> listByGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(expenseService.getExpensesByGroup(groupId));
    }

    @Operation(summary = "그룹에 지출 등록")
    @PostMapping("/groups/{groupId}/expenses")
    public ResponseEntity<ExpenseResponseDto> createUnderGroup(@PathVariable Long groupId,
                                                               @RequestBody ExpenseRequestDto request) {
        // DTO에 groupId 필드가 있다면 여기서 주입 (setter/builder/복사 생성자 등 프로젝트 스타일에 맞게 적용)
        // 예: request.setGroupId(groupId);
        return ResponseEntity.ok(expenseService.createExpense(withGroupId(request, groupId)));
    }

    @Operation(summary = "지출 단건 조회")
    @GetMapping("/groups/{groupId}/expenses/{id}")
    public ResponseEntity<ExpenseResponseDto> get(@PathVariable Long groupId, @PathVariable Long id) {
        // groupId는 보안/권한 검증용으로도 활용 가능
        return ResponseEntity.ok(expenseService.getExpense(id));
    }

    @Operation(summary = "지출 수정")
    @PutMapping("/groups/{groupId}/expenses/{id}")
    public ResponseEntity<ExpenseResponseDto> update(@PathVariable Long groupId,
                                                     @PathVariable Long id,
                                                     @RequestBody ExpenseRequestDto request) {
        return ResponseEntity.ok(expenseService.updateExpense(id, withGroupId(request, groupId)));
    }

    @Operation(summary = "지출 삭제")
    @DeleteMapping("/groups/{groupId}/expenses/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long groupId, @PathVariable Long id) {
        expenseService.deleteExpense(groupId, id);
        return ResponseEntity.noContent().build();
    }


    /* =======================
     * 헬퍼
     * ======================= */
    private ExpenseRequestDto withGroupId(ExpenseRequestDto req, Long groupId) {
        req.setGroupId(groupId);
        return req;
    }
}