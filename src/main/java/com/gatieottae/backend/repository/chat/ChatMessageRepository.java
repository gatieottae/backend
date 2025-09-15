package com.gatieottae.backend.repository.chat;

import com.gatieottae.backend.domain.chat.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findTop50ByGroupIdOrderByIdDesc(Long groupId);

    List<ChatMessage> findTop50ByGroupIdAndIdLessThanOrderByIdDesc(Long groupId, Long beforeId);
}