package com.gatieottae.backend.api.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SendMessageRequestDto {
    @NotBlank
    private String content;           // 메시지 본문
    private List<Long> mentions;      // @멘션 대상 (선택)
    private String localId;           // 클라 임시ID (sending→sent 매칭)
    private String type;              // NORMAL | SYSTEM (기본 NORMAL)
}
