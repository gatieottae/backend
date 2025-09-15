package com.gatieottae.backend.service.notification;

import com.gatieottae.backend.api.notification.dto.NotificationPayloadDto;
import com.gatieottae.backend.infra.notification.RedisNotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final RedisNotificationPublisher publisher;

    /** 특정 수신자에게: 그룹 채팅 알림 */
    public void notifyUserGroupMessage(Long receiverId, Long groupId, Long senderId, String preview) {
        var payload = NotificationPayloadDto.builder()
                .type("MESSAGE")
                .groupId(groupId)
                .receiverId(receiverId)
                .senderId(senderId)
                .title("새 메시지")
                .message(preview)
                .link("/groups/" + groupId + "/chat")
                .sentAt(OffsetDateTime.now().toString())
                .build();

        log.info("[Notif] PUBLISH notif:user:{} -> group {}", receiverId, groupId);
        publisher.publishToUser(receiverId, payload);
    }

    /** 일반 개인 알림 */
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