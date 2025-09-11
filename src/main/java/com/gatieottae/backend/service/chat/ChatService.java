package com.gatieottae.backend.service.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatieottae.backend.api.chat.dto.ChatHistoryResponse;
import com.gatieottae.backend.api.chat.dto.ChatMessageDto;
import com.gatieottae.backend.api.chat.dto.SendMessageRequestDto;
import com.gatieottae.backend.api.chat.dto.SendMessageResponseDto;
import com.gatieottae.backend.domain.chat.ChatMessage;
import com.gatieottae.backend.repository.chat.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 메시지 전송 (DB 저장 + 실시간 브로드캐스트)
     * - 요구: JWT 인증으로부터 memberId 추출 (Controller에서 주입)
     * - mentions는 JSON 문자열로 저장
     */
    @Transactional
    public SendMessageResponseDto send(Long groupId, Long senderId, SendMessageRequestDto req) {
        ChatMessage saved = chatMessageRepository.save(
                ChatMessage.builder()
                        .groupId(groupId)
                        .senderId(senderId)
                        .content(req.getContent())
                        .type(req.getType() != null ? req.getType() : "NORMAL")
                        .mentions(req.getMentions())   // ✅ 그대로 넣기 (문자열 변환 X)
                        .build()
        );

        // 브로드캐스트 페이로드(최소)
        var payload = new ChatBroadcast(
                saved.getId(), groupId, senderId, saved.getContent(),
                saved.getType(), req.getMentions(), saved.getSentAt()
        );

        // /topic/groups/{groupId}/chat 로 전송
        messagingTemplate.convertAndSend("/topic/groups/" + groupId + "/chat", payload);

        return SendMessageResponseDto.builder()
                .id(saved.getId())
                .sentAt(saved.getSentAt())
                .localId(req.getLocalId())
                .build();
    }

    /** 브로드캐스트용 최소 DTO (내부 클래스) */
    public record ChatBroadcast(
            Long id, Long groupId, Long senderId, String content,
            String type, java.util.List<Long> mentions, Instant sentAt
    ) {}

    @Transactional(readOnly = true)
    public ChatHistoryResponse history(Long groupId, Long beforeId, int size) {
        int pageSize = Math.max(1, Math.min(size, 50)); // 1~50 제한
        List<ChatMessage> rows = (beforeId == null)
                ? chatMessageRepository.findTop50ByGroupIdOrderByIdDesc(groupId)
                : chatMessageRepository.findTop50ByGroupIdAndIdLessThanOrderByIdDesc(groupId, beforeId);

        if (rows.size() > pageSize) rows = rows.subList(0, pageSize);

        var list = rows.stream().map(m -> ChatMessageDto.builder()
                .id(m.getId())
                .senderId(m.getSenderId())
                .content(m.getContent())
                .type(m.getType())
                .mentions(m.getMentions())
                .sentAt(m.getSentAt())
                .build()
        ).toList();

        Long next = (list.isEmpty() ? null : list.get(list.size()-1).getId()); // desc라 마지막이 가장 오래된 id
        return ChatHistoryResponse.builder()
                .messages(list)
                .nextCursor(next)
                .build();
    }
}