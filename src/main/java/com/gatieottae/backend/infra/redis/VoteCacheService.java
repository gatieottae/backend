package com.gatieottae.backend.infra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 투표 집계 캐시 레이어.
 *
 * - counts:  H(poll:{id}:counts)   field=optionId(String), value=count(String)
 * - choice:  K(poll:{id}:member:{memberId}) -> optionId(String)
 *
 * PollService 트랜잭션 로직과 호환되도록 시그니처를 맞춰두었습니다.
 */
@Service
@RequiredArgsConstructor
public class VoteCacheService {

    private final StringRedisTemplate redis;

    /* ---------------------- 조회 유틸 ---------------------- */

    /** 현 시점 옵션별 카운트 맵 조회 (없으면 empty) */
    public Map<Long, Long> getCounts(long pollId) {
        String countsKey = VoteCacheKeys.countsKey(pollId);
        Map<Object, Object> raw = redis.opsForHash().entries(countsKey);
        if (raw == null || raw.isEmpty()) return Collections.emptyMap();

        return raw.entrySet().stream().collect(Collectors.toMap(
                e -> Long.parseLong(String.valueOf(e.getKey())),
                e -> Long.parseLong(String.valueOf(e.getValue()))
        ));
    }

    /** 사용자의 현재 선택 조회 (없으면 null) */
    public Long getMemberChoice(long pollId, long memberId) {
        String key = VoteCacheKeys.memberChoiceKey(pollId, memberId);
        String val = redis.opsForValue().get(key);
        if (val == null) return null;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    /* ---------------------- PollService가 기대하는 API ---------------------- */

    /**
     * 투표 반영(교체 포함).
     * PollService에서 DB upsert 성공 후 호출됩니다.
     *
     * @param pollId           폴 ID
     * @param newOptionId      새로 선택된 옵션 ID
     * @param memberId         사용자 ID
     * @param previousOptionId 직전 사용자가 고르고 있던 옵션 ID (없으면 null)
     */
    public void applyVote(long pollId, long newOptionId, long memberId, Long previousOptionId)
            throws DataAccessException {

        String countsKey = VoteCacheKeys.countsKey(pollId);
        String memberKey = VoteCacheKeys.memberChoiceKey(pollId, memberId);

        // 이전과 동일 선택이면 카운트 변화 없음 (DB는 upsert로 동일 상태 유지)
        if (previousOptionId != null && previousOptionId.equals(newOptionId)) {
            // 그래도 멱등하게 choice 키는 세팅해 둔다.
            redis.opsForValue().set(memberKey, String.valueOf(newOptionId));
            return;
        }

        if (previousOptionId != null) {
            // 이전 옵션 -1
            redis.opsForHash().increment(countsKey, String.valueOf(previousOptionId), -1);
        }
        // 새 옵션 +1
        redis.opsForHash().increment(countsKey, String.valueOf(newOptionId), 1);

        // 내 선택 갱신
        redis.opsForValue().set(memberKey, String.valueOf(newOptionId));
    }

    /**
     * 언투표 처리.
     * PollService에서 DB 삭제 성공 후 호출됩니다.
     *
     * @param pollId   폴 ID
     * @param optionId 방금 해제된(이전) 옵션 ID
     * @param memberId 사용자 ID
     */
    public void unvote(long pollId, long optionId, long memberId) throws DataAccessException {
        String countsKey = VoteCacheKeys.countsKey(pollId);
        String memberKey = VoteCacheKeys.memberChoiceKey(pollId, memberId);

        // 해당 옵션 카운트 -1
        redis.opsForHash().increment(countsKey, String.valueOf(optionId), -1);
        // 내 선택 키 제거
        redis.delete(memberKey);
    }

    /**
     * 결과 캐시 조회 (없으면 Optional.empty()).
     * - counts가 비어 있으면 "캐시 미스"로 간주합니다.
     * - myOptionId는 없을 수도 있습니다(null 허용).
     */
    public Optional<CachedResults> tryGetResults(long pollId, long memberId) {
        Map<Long, Long> counts = getCounts(pollId);
        if (counts.isEmpty()) return Optional.empty(); // 캐시 미스

        Long my = getMemberChoice(pollId, memberId);
        return Optional.of(new CachedResults(counts, my));
    }

    /**
     * 캐시 워밍업.
     * - DB에서 계산한 counts와 (가능하면) 내 선택을 캐시에 적재
     * - TTL 전략이 필요하면 여기에서 expire 설정을 추가하세요.
     */
    public void warmUp(long pollId, Map<Long, Long> counts, Long memberId, Long myOptionId) {
        if (counts != null && !counts.isEmpty()) {
            String countsKey = VoteCacheKeys.countsKey(pollId);
            Map<String, String> asString = new HashMap<>();
            counts.forEach((k, v) -> asString.put(String.valueOf(k), String.valueOf(v)));
            redis.opsForHash().putAll(countsKey, new HashMap<>(asString));
        }
        if (memberId != null && myOptionId != null) {
            String memberKey = VoteCacheKeys.memberChoiceKey(pollId, memberId);
            redis.opsForValue().set(memberKey, String.valueOf(myOptionId));
        }
    }

    /**
     * 캐시 비움 (마감 시 등).
     * - counts 해시만 제거합니다.
     * - member 키들은 TTL 전략을 권장(대량 SCAN 삭제는 운영에서 별도 처리)
     */
    public void evict(long pollId) {
        redis.delete(VoteCacheKeys.countsKey(pollId));
    }

    /* ---------------------- 보조 타입 ---------------------- */

    /** results() 캐시 응답 컨테이너 */
    public record CachedResults(Map<Long, Long> counts, Long myOptionId) {}
}