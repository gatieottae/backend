package com.gatieottae.backend.api.chat.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatHistoryResponse {
    private List<ChatMessageDto> messages;  // 최신순(desc)로 내려줌
    private Long nextCursor;                // 더 불러올 때 사용할 beforeId (null이면 끝)
}