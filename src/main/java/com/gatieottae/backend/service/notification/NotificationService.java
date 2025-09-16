package com.gatieottae.backend.service.notification;

import com.gatieottae.backend.api.notification.dto.NotificationPayloadDto;
import com.gatieottae.backend.infra.notification.RedisNotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

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

    /** 보채기 */
    public void sendNudge(Long targetMemberId, String message) {
        var payload = NotificationPayloadDto.builder()
                .type("NUDGE")
                .receiverId(targetMemberId)
                .title("정산 보채기")
                .message(message) // 예: "장소희님이 2,000원을 아직 송금하지 않았습니다."
                .sentAt(OffsetDateTime.now().toString())
                .build();

        log.info("[Notif] PUBLISH nudge to user: {}", targetMemberId);
        publisher.publishToUser(targetMemberId, payload);
    }

    /** 송금자가 돈을 '보냈어요' 했을 때 → 수신자에게 알림 */
    public void notifySent(Long toMemberId, Long transferId, Long amount, Long groupId, Long fromMemberId) {
        var payload = NotificationPayloadDto.builder()
                .type("TRANSFER_SENT")
                .receiverId(toMemberId)
                .groupId(groupId)
                .transferId(transferId)
                .senderId(fromMemberId)
                .title("입금 예정 알림")
                .message(String.format("%d원을 보냈습니다. (송금ID: %d)", amount, transferId))
                .sentAt(OffsetDateTime.now().toString())
                .build();

        publisher.publishToUser(toMemberId, payload);
    }

    /** 수신자가 '받았어요(확인)' 했을 때 → 송금자에게 알림 */
    public void notifyConfirmed(Long fromMemberId, Long id, Long amount) {
        var payload = NotificationPayloadDto.builder()
                .type("TRANSFER_CONFIRMED")
                .receiverId(fromMemberId)
                .title("입금 확인")
                .message(String.format("상대가 %d원 입금을 확인했습니다. (송금ID: %d)", amount, id))
                .sentAt(OffsetDateTime.now().toString())
                .build();

        log.info("[Notif] PUBLISH transfer confirmed -> to sender: {}, transferId: {}", fromMemberId, id);
        publisher.publishToUser(fromMemberId, payload);
    }

    /**
     * 송금 롤백 알림
     * NOTE: 파라미터명이 fromMemberId로 되어 있지만, 실질적으로는 '수신자 목록'이므로 그대로 사용.
     *       필요하면 시그니처를 recipients 같은 이름으로 리팩터링 권장.
     */
    public void notifyRolledBack(List<Long> fromMemberId, Long id, Long amount) {
        var payload = NotificationPayloadDto.builder()
                .type("TRANSFER_ROLLED_BACK")
                .title("송금 롤백")
                .message(String.format("송금이 롤백되었습니다. 금액: %d원 (송금ID: %d)", amount, id))
                .sentAt(OffsetDateTime.now().toString())
                .build();

        // 모든 대상자에게 브로드캐스트
        for (Long receiverId : fromMemberId) {
            if (receiverId == null) continue;
            var personal = payload.toBuilder().receiverId(receiverId).build();
            log.info("[Notif] PUBLISH transfer rolled back -> user: {}, transferId: {}", receiverId, id);
            publisher.publishToUser(receiverId, personal);
        }
    }

    /** 증빙 첨부/수정 알림 → 수신자에게 전송 */
    public void notifyProofAttached(Long toMemberId, Long id, String proofUrl) {
        var payload = NotificationPayloadDto.builder()
                .type("TRANSFER_PROOF_ATTACHED")
                .receiverId(toMemberId)
                .title("증빙 첨부")
                .message(String.format("송금 증빙이 첨부되었습니다. (송금ID: %d)", id))
                .sentAt(OffsetDateTime.now().toString())
                .build();

        log.info("[Notif] PUBLISH transfer proof attached -> to: {}, transferId: {}, proofUrl: {}",
                toMemberId, id, proofUrl);
        publisher.publishToUser(toMemberId, payload);
    }

}