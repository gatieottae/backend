package com.gatieottae.backend.api.chat.controller;

import com.gatieottae.backend.api.chat.dto.SendMessageRequestDto;
import com.gatieottae.backend.api.chat.dto.SendMessageResponseDto;
import com.gatieottae.backend.service.chat.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Chat", description = "그룹 단일 채팅 스레드 API")
@RestController
@RequestMapping("/api/chat/groups/{groupId}")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "메시지 전송 (저장 + 브로드캐스트)")
    @PostMapping("/messages")
    public ResponseEntity<SendMessageResponseDto> send(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "id") Long memberId, // UserPrincipal.id 형태 사용 중
            @Valid @RequestBody SendMessageRequestDto req
    ) {
        var res = chatService.send(groupId, memberId, req);
        return ResponseEntity.ok(res);
    }
}