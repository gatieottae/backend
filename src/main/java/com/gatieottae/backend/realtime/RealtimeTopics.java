package com.gatieottae.backend.realtime;

public final class RealtimeTopics {
    private RealtimeTopics() {}

    // 그룹 단위 (지출/정산)
    public static String expenseTopic(Long groupId) {
        return "/topic/groups/" + groupId + "/expense";
    }
    public static String settlementTopic(Long groupId) {
        return "/topic/groups/" + groupId + "/settlement";
    }

    // 사용자 단위 (알림/버튼 상태)
    public static String userNotifTopic(Long memberId) {
        return "/topic/notif/user/" + memberId;
    }
}