package com.gatieottae.backend.api.notification.dto;

import lombok.Builder;

@Builder
public record NotificationPayloadDto(
        String type,        // "MESSAGE" | "SCHEDULE" | ...
        Long groupId,       // 그룹 알림이면 필수
        Long receiverId,    // 개인 알림이면 사용 (옵션)
        Long senderId,
        String title,       // UI 뱃지/토스트 타이틀
        String message,     // 미리보기 텍스트
        String sentAt,      // ISO-8601 문자열
        String link         // 클릭 시 이동할 경로 (예: /groups/{id})
) {}