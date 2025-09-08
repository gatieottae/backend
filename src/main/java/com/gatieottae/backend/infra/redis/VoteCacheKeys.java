package com.gatieottae.backend.infra.redis;

/**
 * Redis 키 네이밍을 한 곳에서 관리합니다.
 *  - poll:{pollId}:counts         (HASH) : 옵션별 득표수
 *  - poll:{pollId}:member:{uid}   (STRING): 특정 사용자가 고른 옵션ID
 */
public final class VoteCacheKeys {

    private VoteCacheKeys() {}

    public static String countsKey(long pollId) {
        return "poll:" + pollId + ":counts";
    }

    public static String memberChoiceKey(long pollId, long memberId) {
        return "poll:" + pollId + ":member:" + memberId;
    }
}