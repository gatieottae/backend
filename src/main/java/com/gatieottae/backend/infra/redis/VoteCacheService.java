package com.gatieottae.backend.infra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 투표 집계 캐시 레이어.
 *
 * - counts:  H(poll:{id}:counts)   field=optionId(String), value=count(String)
 * - choice:  K(poll:{id}:member:{memberId}) -> optionId(String)
 *
 * TTL 전략:
 * - open 상태의 poll 은 closesAt(마감시각) 기준 TTL 적용.
 * - closesAt 없으면 보수적 기본 TTL.
 * - 마감(close) 시 counts 해시 즉시 삭제, choice 키는 TTL로 자연 만료.
 */
@Service
@RequiredArgsConstructor
public class VoteCacheService {
    private final StringRedisTemplate redis;

    private static final Duration DEFAULT_TTL_IF_NO_CLOSES_AT = Duration.ofHours(24);
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    /* ================= 조회 ================= */

    public Map<Long, Long> getCounts(long pollId) {
        String countsKey = VoteCacheKeys.countsKey(pollId);
        Map<Object, Object> raw = redis.opsForHash().entries(countsKey);
        if (raw == null || raw.isEmpty()) return Collections.emptyMap();

        return raw.entrySet().stream().collect(Collectors.toMap(
                e -> Long.parseLong(String.valueOf(e.getKey())),
                e -> Long.parseLong(String.valueOf(e.getValue()))
        ));
    }

    public Long getMemberChoice(long pollId, long memberId) {
        String key = VoteCacheKeys.memberChoiceKey(pollId, memberId);
        String val = redis.opsForValue().get(key);
        if (val == null) return null;
        try { return Long.parseLong(val); } catch (NumberFormatException ignore) { return null; }
    }

    public Long getTtlForCounts(long pollId) {
        String countsKey = VoteCacheKeys.countsKey(pollId);
        return redis.getExpire(countsKey); // 초 단위 TTL 반환
    }

    /* ================= 변경(필수 인자: closesAt) ================= */

    /**
     * 투표 반영(교체 포함). DB upsert 성공 후 호출.
     */
    public void applyVote(long pollId, long newOptionId, long memberId, Long previousOptionId, OffsetDateTime closesAt)
            throws DataAccessException {
        String countsKey = VoteCacheKeys.countsKey(pollId);
        String memberKey = VoteCacheKeys.memberChoiceKey(pollId, memberId);

        // 동일 선택이면 카운트 변화 없음. choice는 TTL과 함께 갱신, counts TTL 터치.
        if (previousOptionId != null && previousOptionId.equals(newOptionId)) {
            redis.opsForValue().set(memberKey, String.valueOf(newOptionId), resolveTtl(closesAt));
            touchCountsTtl(countsKey, closesAt);
            return;
        }

        if (previousOptionId != null) {
            redis.opsForHash().increment(countsKey, String.valueOf(previousOptionId), -1);
        }
        redis.opsForHash().increment(countsKey, String.valueOf(newOptionId), 1);

        redis.opsForValue().set(memberKey, String.valueOf(newOptionId), resolveTtl(closesAt));
        touchCountsTtl(countsKey, closesAt);
    }

    /**
     * 언투표. DB 삭제 성공 후 호출.
     */
    public void unvote(long pollId, long optionId, long memberId, OffsetDateTime closesAt) throws DataAccessException {
        String countsKey = VoteCacheKeys.countsKey(pollId);
        String memberKey = VoteCacheKeys.memberChoiceKey(pollId, memberId);

        redis.opsForHash().increment(countsKey, String.valueOf(optionId), -1);
        // 언투표는 내 선택 키 즉시 삭제
        redis.delete(memberKey);
        touchCountsTtl(countsKey, closesAt);
    }

    /**
     * 결과 캐시 조회 (없으면 Optional.empty()).
     */
    public Optional<CachedResults> tryGetResults(long pollId, long memberId) {
        Map<Long, Long> counts = getCounts(pollId);
        if (counts.isEmpty()) return Optional.empty();
        Long my = getMemberChoice(pollId, memberId);
        return Optional.of(new CachedResults(counts, my));
    }

    /**
     * 캐시 워밍업. DB에서 계산한 counts와 내 선택을 캐시에 적재(+ TTL).
     */
    public void warmUp(long pollId, Map<Long, Long> counts, Long memberId, Long myOptionId, OffsetDateTime closesAt) {
        if (counts != null && !counts.isEmpty()) {
            String countsKey = VoteCacheKeys.countsKey(pollId);
            Map<String, String> asString = new HashMap<>();
            counts.forEach((k, v) -> asString.put(String.valueOf(k), String.valueOf(v)));
            redis.opsForHash().putAll(countsKey, new HashMap<>(asString));
            touchCountsTtl(countsKey, closesAt);
        }
        if (memberId != null && myOptionId != null) {
            String memberKey = VoteCacheKeys.memberChoiceKey(pollId, memberId);
            redis.opsForValue().set(memberKey, String.valueOf(myOptionId), resolveTtl(closesAt));
        }
    }

    /**
     * 마감 시 캐시 비움.
     * - counts 해시만 즉시 삭제.
     * - member choice 키는 TTL로 방치(필요 시 운영 스크립트로 패턴 정리).
     */
    public void evictOnClose(long pollId) {
        redis.delete(VoteCacheKeys.countsKey(pollId));
    }

    // 기존 호출 호환용 (closesAt 없이)
    public void applyVote(long pollId, long newOptionId, long memberId, Long previousOptionId) {
        applyVote(pollId, newOptionId, memberId, previousOptionId, null);
    }

    public void unvote(long pollId, long optionId, long memberId) {
        unvote(pollId, optionId, memberId, null);
    }

    /* ================= TTL Helper ================= */

    private Duration resolveTtl(OffsetDateTime closesAt) {
        final Duration GRACE = Duration.ofMinutes(5);
        if (closesAt == null) return DEFAULT_TTL_IF_NO_CLOSES_AT;

        OffsetDateTime now = OffsetDateTime.now(ZONE_ID);
        if (closesAt.isAfter(now)) {
            Duration ttl = Duration.between(now, closesAt).plus(GRACE);
            return ttl.isNegative() ? Duration.ofSeconds(1) : ttl;
        }
        // 이미 지난 마감: 짧게 유지
        return Duration.ofMinutes(5);
    }

    private void touchCountsTtl(String countsKey, OffsetDateTime closesAt) {
        Duration ttl = resolveTtl(closesAt);
        redis.expire(countsKey, ttl);
    }

    /** results() 캐시 응답 컨테이너 */
    public record CachedResults(Map<Long, Long> counts, Long myOptionId) {}
}