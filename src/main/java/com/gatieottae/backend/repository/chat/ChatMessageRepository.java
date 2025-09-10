package com.gatieottae.backend.repository.chat;

import com.gatieottae.backend.domain.chat.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // 전송은 save()만 필요. 히스토리는 다음 단계에서 쿼리 추가.
}