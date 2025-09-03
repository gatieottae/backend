package com.gatieottae.backend.api.group;

import com.gatieottae.backend.api.group.dto.GroupRequestDto;
import com.gatieottae.backend.api.group.dto.GroupResponseDto;
import com.gatieottae.backend.domain.group.GroupService;
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

    @PostMapping
    public ResponseEntity<GroupResponseDto> createGroup(
            @Valid @RequestBody GroupRequestDto request,
            // ✅ JwtAuthenticationFilter가 넣어준 LoginMember의 id를 바로 꺼내씀
            @AuthenticationPrincipal(expression = "id") Long userId
    ) {
        GroupResponseDto response = groupService.createGroup(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}