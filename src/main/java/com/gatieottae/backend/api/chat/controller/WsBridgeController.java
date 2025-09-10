package com.gatieottae.backend.api.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Tag(name = "WS Test", description = "Swagger에서 WS 브로드캐스트를 유발하는 테스트 API")
@RestController
@RequestMapping("/api/ws")
@RequiredArgsConstructor
public class WsBridgeController {

    private final SimpMessagingTemplate messagingTemplate;

    @Operation(summary = "테스트 메시지 브로드캐스트",
            description = "Swagger에서 호출하면 /topic/test 로 서버가 푸시합니다.")
    @PostMapping("/broadcast")
    public ResponseEntity<Out> broadcast(
            @RequestBody In req,
             @AuthenticationPrincipal(expression = "id") Long auth
    ) {
        Out payload = new Out("echo: " + req.getText(), auth, Instant.now().toString());

        // 실제 WS 브로드캐스트
        messagingTemplate.convertAndSend("/topic/test", payload);

        // 호출자에게도 응답으로 회신
        return ResponseEntity.ok(payload);
    }

    @Data
    public static class In { @NotBlank private String text; }
    @Data
    public static class Out {
        private final String text;
        private final Long fromMemberId;
        private final String at;
    }
}