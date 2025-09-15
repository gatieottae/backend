package com.gatieottae.backend.infra.notification;

/**
 * Redis Pub/Sub 채널 네이밍 규칙과 패턴 집합
 * - 기존 notif:* 유지
 * - transfers:* (정산/송금 알림) 추가
 */
public final class NotificationTopics {
    private NotificationTopics() {}

    public static final String PREFIX_NOTIF_GROUP = "notif:group:"; // notif:group:{groupId}
    public static final String PREFIX_NOTIF_USER  = "notif:user:";  // notif:user:{memberId}
    public static final String PATTERN_ALL        = "notif:*";

    public static String groupTopic(Long groupId) { return PREFIX_NOTIF_GROUP + groupId; }
    public static String userTopic(Long memberId) { return PREFIX_NOTIF_USER + memberId; }

    public static final String PREFIX_TRANSFERS_GROUP = "transfers:"; // transfers:{groupId}
    public static final String PATTERN_TRANSFERS_ALL  = "transfers:*";

    /** 정산/송금 이벤트 채널명: transfers:{groupId} */
    public static String transfersTopic(Long groupId) { return PREFIX_TRANSFERS_GROUP + groupId; }

    /** WebSocket destination (group broadcast): /topic/groups/{groupId}/transfers */
    public static String wsTransfersDestination(Long groupId) {
        return "/topic/groups/" + groupId + "/transfers";
    }

    /** 개인 알림을 transfers 이벤트로도 내려주고 싶을 때 재사용 (기존 규칙과 일관성) */
    public static String wsUserNotificationDestination(Long memberId) {
        return "/topic/notifications/" + memberId;
    }
}