package com.gatieottae.backend.infra.redis;

import com.gatieottae.backend.infra.redis.VoteCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 내부 점검용 엔드포인트입니다. (운영 노출 금지)
 *  - POST /internal/redis/vote-cache/apply-vote?pollId=&memberId=&optionId=
 *  - DELETE /internal/redis/vote-cache/unvote?pollId=&memberId=
 *  - GET /internal/redis/vote-cache/counts?pollId=
 *  - GET /internal/redis/vote-cache/member-choice?pollId=&memberId=
 */
@RestController
@RequestMapping("/internal/redis/vote-cache")
@RequiredArgsConstructor
public class VoteCacheDebugController {

    private final VoteCacheService voteCache;

    @PostMapping("/apply-vote")
    public ResponseEntity<Void> applyVote(
            @RequestParam long pollId,
            @RequestParam long memberId,
            @RequestParam long optionId
    ) {
        Long prev = voteCache.getMemberChoice(pollId, memberId);
        voteCache.applyVote(pollId, optionId, memberId, prev);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/unvote")
    public ResponseEntity<Void> unvote(
            @RequestParam long pollId,
            @RequestParam long memberId
    ) {
        Long prev = voteCache.getMemberChoice(pollId, memberId);
        if (prev != null) {
            voteCache.unvote(pollId, prev, memberId);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/counts")
    public ResponseEntity<Map<Long, Long>> counts(@RequestParam long pollId) {
        return ResponseEntity.ok(voteCache.getCounts(pollId));
    }

    @GetMapping("/member-choice")
    public ResponseEntity<Long> memberChoice(
            @RequestParam long pollId,
            @RequestParam long memberId
    ) {
        return ResponseEntity.ok(voteCache.getMemberChoice(pollId, memberId));
    }

    @GetMapping("/ttl")
    public ResponseEntity<Long> ttl(@RequestParam long pollId) {
        Long ttl = voteCache.getTtlForCounts(pollId);
        return ResponseEntity.ok(ttl);
    }
}