package com.gatieottae.backend.api.me;

import com.gatieottae.backend.api.me.dto.CursorPageResponse;
import com.gatieottae.backend.api.me.dto.MyGroupItemDto;
import com.gatieottae.backend.domain.group.MyGroupsService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me")
public class MyGroupsController {

    private final MyGroupsService service;

    @Operation(summary = "내 그룹 리스트 조회(커서 기반)")
    @GetMapping("/groups")
    public ResponseEntity<CursorPageResponse<MyGroupItemDto>> getMyGroups(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,  // before|during|after
            @RequestParam(required = false, defaultValue = "startAsc") String sort,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @RequestParam(required = false) String cursor
    ){
        return ResponseEntity.ok(
                service.getMyGroups(userId, q, status, sort, size, cursor)
        );
    }
}