package com.gatieottae.backend.infra.redis;

import com.gatieottae.backend.domain.poll.Poll;
import com.gatieottae.backend.domain.poll.PollStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoteBroadcastCoordinator {

    private final VoteCacheService cache;          // 이미 구현되어 있음
    private final VoteBroadcastPublisher publisher;

    /**
     * 현재 캐시에 있는 counts 스냅샷을 읽어 WS 브로드캐스트 트리거.
     * - 캐시가 비어있으면(미스) 조용히 skip
     */
    public void broadcastCountsSnapshot(Poll poll) {
        Map<Long, Long> counts = cache.getCounts(poll.getId());
        if (counts == null || counts.isEmpty()) return;

        String status = (poll.getStatus() == null) ? PollStatus.OPEN.name() : poll.getStatus().name();
        publisher.publishSnapshot(poll.getId(), status, counts);
    }
}