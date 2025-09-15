package com.gatieottae.backend.api.chat.dto;

import lombok.*;
import java.time.Instant;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessageDto {
    private Long id;
    private Long senderId;
    private String content;
    private String type;                 // NORMAL | SYSTEM
    private List<Long> mentions;
    private Instant sentAt;
}