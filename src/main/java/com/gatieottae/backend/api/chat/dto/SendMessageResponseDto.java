package com.gatieottae.backend.api.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.Instant;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SendMessageResponseDto {
    private Long id;                  // serverId (DB PK)
    private Instant sentAt;           // 서버 기준 전송 시각
    private String localId;           // 클라가 보낸 localId 에코백
}