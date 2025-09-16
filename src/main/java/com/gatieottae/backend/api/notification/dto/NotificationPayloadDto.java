package com.gatieottae.backend.api.notification.dto;

import lombok.Builder;

@Builder(toBuilder = true)
public record NotificationPayloadDto(
        String type,        // "MESSAGE" | "TRANSFER_SENT" | ...
        Long groupId,       // 그룹 알림이면 필수
        Long transferId,    // 송금 ID
        Long receiverId,    // 개인 알림이면 사용 (옵션)
        Long senderId,      // 송금 보낸 사람
        String title,       // UI 뱃지/토스트 타이틀
        String message,     // 미리보기 텍스트
        String sentAt,      // ISO-8601 문자열
        String link         // 클릭 시 이동할 경로 (예: /groups/{id})
) {}