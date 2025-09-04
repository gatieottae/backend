package com.gatieottae.backend.api.group.controller;

import com.gatieottae.backend.api.group.dto.GroupDetailResponseDto;
import com.gatieottae.backend.domain.group.GroupDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups")
public class GroupDetailController {

    private final GroupDetailService service;

    @GetMapping("/{groupId}")
    public GroupDetailResponseDto getGroupDetail(@PathVariable Long groupId,
                                                 @AuthenticationPrincipal(expression = "id") Long userId) {
        return service.getGroupDetail(groupId);
    }
}