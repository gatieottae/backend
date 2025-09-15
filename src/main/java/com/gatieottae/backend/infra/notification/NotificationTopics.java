package com.gatieottae.backend.infra.notification;

public final class NotificationTopics {
    private NotificationTopics() {}
    public static String groupTopic(Long groupId) { return "notif:group:" + groupId; }
    public static String userTopic(Long memberId) { return "notif:user:"  + memberId; }
    public static final String PATTERN_ALL = "notif:*"; // 구독 패턴
}