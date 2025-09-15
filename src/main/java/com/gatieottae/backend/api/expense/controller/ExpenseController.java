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
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @Operation(summary = "지출 등록")
    @PostMapping
    public ResponseEntity<ExpenseResponseDto> create(@RequestBody ExpenseRequestDto request) {
        return ResponseEntity.ok(expenseService.createExpense(request));
    }

    @Operation(summary = "지출 단건 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponseDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(expenseService.getExpense(id));
    }

    @Operation(summary = "그룹별 지출 목록 조회")
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<ExpenseResponseDto>> list(@PathVariable Long groupId) {
        return ResponseEntity.ok(expenseService.getExpensesByGroup(groupId));
    }

    @Operation(summary = "지출 수정")
    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponseDto> update(@PathVariable Long id, @RequestBody ExpenseRequestDto request) {
        return ResponseEntity.ok(expenseService.updateExpense(id, request));
    }

    @Operation(summary = "지출 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }
}