package com.gatieottae.backend.service.notification;

import com.gatieottae.backend.api.notification.dto.NotificationPayloadDto;
import com.gatieottae.backend.infra.notification.RedisNotificationPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final RedisNotificationPublisher publisher;

    public void notifyGroupMessage(Long groupId, Long senderId, String preview) {
        var payload = NotificationPayloadDto.builder()
                .type("MESSAGE")
                .groupId(groupId)
                .senderId(senderId)
                .title("새 메시지")
                .message(preview)
                .link("/groups/" + groupId + "/chat")
                .sentAt(OffsetDateTime.now().toString())
                .build();

        publisher.publishToGroup(groupId, payload);
    }

    // 필요 시: 특정 유저 대상으로
    public void notifyUser(Long memberId, String title, String message, String link) {
        var payload = NotificationPayloadDto.builder()
                .type("GENERAL")
                .receiverId(memberId)
                .title(title)
                .message(message)
                .link(link)
                .sentAt(OffsetDateTime.now().toString())
                .build();

        publisher.publishToUser(memberId, payload);
    }
}