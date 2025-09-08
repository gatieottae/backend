package com.gatieottae.backend.infra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 투표 집계를 위한 Redis 접근 레이어.
 * [핵심 연산]
 *  - applyVote(pollId, memberId, optionId): 교체/신규 투표에 따라 HINCRBY +/- 수행
 *  - unvote(pollId, memberId): 사용자의 기존 선택이 있으면 해당 옵션 카운트를 -1
 *  - getCounts(pollId): 현 시점 옵션별 카운트 맵 조회
 *  - getMemberChoice(pollId, memberId): 사용자의 현재 선택 조회
 *
 * 주의: 이 레이어는 "캐시/성능"을 위한 것이며,
 *       최종 정합성은 DB 트랜잭션 로직(PollService)에서 보장하는 구조가 좋습니다.
 *       실제 연결은 3단계에서 PollService에 붙입니다.
 */
@Service
@RequiredArgsConstructor
public class VoteCacheService {

    private final StringRedisTemplate redis;

    /** 사용자의 기존 선택을 읽어옵니다. 없으면 null */
    public Long getMemberChoice(long pollId, long memberId) {
        String key = VoteCacheKeys.memberChoiceKey(pollId, memberId);
        String val = redis.opsForValue().get(key);
        if (val == null) return null;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 투표 반영(교체 포함).
     * - 이전 선택(prev)이 없으면: 새 옵션 +1
     * - 이전 선택이 새 옵션과 다르면: prev -1, 새 옵션 +1
     * - 이전 선택과 동일하면: 변경 없음(“같은 항목 재클릭=언투표”는 외부에서 별도로 unvote() 호출)
     */
    public void applyVote(long pollId, long memberId, long optionId) throws DataAccessException {
        String countsKey = VoteCacheKeys.countsKey(pollId);
        String memberKey = VoteCacheKeys.memberChoiceKey(pollId, memberId);

        Long prev = getMemberChoice(pollId, memberId);

        // 동일 선택이면 여기서는 아무것도 하지 않음(프론트/서비스에서 unvote를 따로 호출하도록)
        if (prev != null && prev == optionId) return;

        // 파이프라인처럼 보이지만 간단히 순차 처리
        if (prev != null) {
            // 이전 옵션 -1
            redis.opsForHash().increment(countsKey, String.valueOf(prev), -1);
        }
        // 새 옵션 +1
        redis.opsForHash().increment(countsKey, String.valueOf(optionId), 1);

        // 내 선택 갱신
        redis.opsForValue().set(memberKey, String.valueOf(optionId));
    }

    /**
     * 언투표 처리:
     * - 이전 선택이 있으면 해당 옵션 카운트 -1
     * - 내 선택 키 삭제
     * - 이전 선택이 없으면 멱등하게 아무일도 안 함
     */
    public void unvote(long pollId, long memberId) throws DataAccessException {
        String countsKey = VoteCacheKeys.countsKey(pollId);
        String memberKey = VoteCacheKeys.memberChoiceKey(pollId, memberId);

        Long prev = getMemberChoice(pollId, memberId);
        if (prev == null) return; // 멱등

        redis.opsForHash().increment(countsKey, String.valueOf(prev), -1);
        redis.delete(memberKey);
    }

    /** 현 시점 옵션별 카운트 맵 */
    public Map<Long, Long> getCounts(long pollId) {
        String countsKey = VoteCacheKeys.countsKey(pollId);
        Map<Object, Object> raw = redis.opsForHash().entries(countsKey);
        if (raw == null || raw.isEmpty()) return Collections.emptyMap();

        return raw.entrySet().stream().collect(Collectors.toMap(
                e -> Long.parseLong(String.valueOf(e.getKey())),
                e -> Long.parseLong(String.valueOf(e.getValue()))
        ));
    }

    /** 특정 옵션 카운트를 명시적으로 보정(테스트/관리용) */
    public void setCount(long pollId, long optionId, long count) {
        String countsKey = VoteCacheKeys.countsKey(pollId);
        redis.opsForHash().put(countsKey, String.valueOf(optionId), String.valueOf(count));
    }

    /** 해당 poll의 캐시 데이터를 모두 비웁니다(테스트/마감 플러시 전 초기화 등). */
    public void clearAll(long pollId) {
        redis.delete(VoteCacheKeys.countsKey(pollId));
        // member 키는 패턴 삭제가 필요하지만 Redis 단건 delete는 패턴을 직접 못 쓰므로
        // 운영에서는 SCAN으로 지우거나, 마감시점에 TTL로 자연 만료시키는 전략을 권장합니다.
    }
}