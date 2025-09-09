package com.gatieottae.backend.service.poll;

import com.gatieottae.backend.api.poll.dto.PollDto;
import com.gatieottae.backend.common.exception.ConflictException;
import com.gatieottae.backend.common.exception.NotFoundException;
import com.gatieottae.backend.domain.poll.*;
import com.gatieottae.backend.infra.redis.VoteCacheService;
import com.gatieottae.backend.repository.poll.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PollService {

    private final PollRepository pollRepo;
    private final PollCategoryRepository categoryRepo;
    private final PollOptionRepository optionRepo;
    private final PollVoteRepository voteRepo;
    private final VoteCacheService voteCache;

    @Transactional
    public PollDto.CreateRes create(Long memberId, PollDto.CreateReq req) {
        if (req.groupId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "groupId is required");
        if (req.title() == null || req.title().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required");
        if (req.categoryCode() == null || req.categoryCode().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoryCode is required");
        if (req.options() == null || req.options().isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "options is required");

        PollCategory cat = categoryRepo.findByCode(req.categoryCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "unknown categoryCode"));

        OffsetDateTime now = OffsetDateTime.now();

        Poll poll = Poll.builder()
                .groupId(req.groupId())
                .category(cat)
                .title(req.title().trim())
                .description(req.description())
                .status(PollStatus.OPEN)
                .closesAt(req.closesAt())
                .createdBy(memberId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // 옵션 추가
        List<PollOption> options = new ArrayList<>();
        int order = 0;
        for (String content : req.options()) {
            if (content == null || content.isBlank()) continue;
            options.add(PollOption.builder()
                    .poll(poll)
                    .content(content.trim())
                    .sortOrder(order++)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        }
        poll.setOptions(options);

        pollRepo.save(poll); // cascade로 option 저장

        return new PollDto.CreateRes(poll.getId());
    }

    @Transactional
    public void vote(Long pollId, Long memberId, Long optionId) {
        Poll poll = pollRepo.findById(pollId)
                .orElseThrow(() -> new NotFoundException("poll not found"));
        if (poll.getStatus() != PollStatus.OPEN) {
            throw new ConflictException("poll closed");
        }

        PollOption opt = optionRepo.findById(optionId)
                .orElseThrow(() -> new NotFoundException("option not found"));
        if (!opt.getPoll().getId().equals(pollId)) {
            throw new ConflictException("option not in this poll");
        }

        // 캐시 정확도를 위해 커밋 전에 "기존 내 선택"을 읽어둔다
        Long previousOptionId = voteRepo.findByPoll_IdAndMemberId(pollId, memberId)
                .map(v -> v.getOption().getId())
                .orElse(null);

        // DB upsert
        voteRepo.upsertVote(pollId, optionId, memberId);


        final long pId = pollId, mId = memberId, optId = optionId;
        final Long prev = previousOptionId;
        final OffsetDateTime closesAt = poll.getClosesAt();
        runAfterCommit(() -> voteCache.applyVote(pId, optId, mId, prev, closesAt));

    }

    @Transactional(readOnly = true)
    public PollDto.ResultsRes results(Long pollId, Long memberId) {
        // 1) ✅ 캐시 먼저 시도
        var cached = voteCache.tryGetResults(pollId, memberId);
        if (cached.isPresent()) {
            var c = cached.get();

            // 옵션 목록은 DB에서 정렬/문구를 가져오되, 득표수는 캐시의 count 사용
            Poll poll = pollRepo.findById(pollId)
                    .orElseThrow(() -> new NotFoundException("poll not found"));

            var options = optionRepo.findByPollIdOrderBySortOrderAscIdAsc(pollId);
            List<PollDto.ResultsRes.OptionResult> list = new ArrayList<>();
            for (PollOption opt : options) {
                long cnt = c.counts().getOrDefault(opt.getId(), 0L);
                boolean isMine = c.myOptionId() != null && c.myOptionId().equals(opt.getId());
                list.add(new PollDto.ResultsRes.OptionResult(opt.getId(), opt.getContent(), cnt, isMine));
            }

            return new PollDto.ResultsRes(
                    poll.getId(),
                    poll.getTitle(),
                    poll.getCategory().getCode(),
                    poll.getStatus().name(),
                    poll.getClosesAt(),
                    list,
                    c.myOptionId() != null
            );
        }

        // 2) ❄️ 캐시에 없으면 DB로 계산 (기존 로직)
        Poll poll = pollRepo.findById(pollId)
                .orElseThrow(() -> new NotFoundException("poll not found"));

        var options = optionRepo.findByPollIdOrderBySortOrderAscIdAsc(pollId);
        var myVote = voteRepo.findByPoll_IdAndMemberId(pollId, memberId).orElse(null);

        // optionId -> count 맵 구성
        var counts = new java.util.HashMap<Long, Long>();
        for (PollOption opt : options) {
            long cnt = voteRepo.countByPollIdAndOptionId(pollId, opt.getId());
            counts.put(opt.getId(), cnt);
        }
        Long myOptionId = (myVote == null) ? null : myVote.getOption().getId();

        // 3) ✅ 캐시 워밍업
        voteCache.warmUp(pollId, counts, memberId, myOptionId, poll.getClosesAt());

        // 4) 응답 DTO 생성
        List<PollDto.ResultsRes.OptionResult> list = new ArrayList<>();
        for (PollOption opt : options) {
            long cnt = counts.getOrDefault(opt.getId(), 0L);
            boolean isMine = myOptionId != null && myOptionId.equals(opt.getId());
            list.add(new PollDto.ResultsRes.OptionResult(opt.getId(), opt.getContent(), cnt, isMine));
        }

        return new PollDto.ResultsRes(
                poll.getId(),
                poll.getTitle(),
                poll.getCategory().getCode(),
                poll.getStatus().name(),
                poll.getClosesAt(),
                list,
                myOptionId != null
        );
    }

    @Transactional(readOnly = true)
    public List<PollDto.ListItem> list(Long groupId, Long memberId) {
        var polls = pollRepo.findByGroupIdOrderByCreatedAtDesc(groupId);
        if (polls.isEmpty()) return List.of();

        var pollIds = polls.stream().map(Poll::getId).toList();
        var myVotes = new java.util.HashMap<Long, Long>();
        for (var v : voteRepo.findByPoll_IdInAndMemberId(pollIds, memberId)) {
            myVotes.put(v.getPoll().getId(), v.getOption().getId());
        }

        var result = new ArrayList<PollDto.ListItem>(polls.size());

        for (var p : polls) {
            final long pollId = p.getId();
            final boolean isOpen = p.getStatus() == PollStatus.OPEN;

            // 캐시 먼저
            java.util.Map<Long, Long> countsMap = null; // ← import 없으면 이렇게 fully-qualified
            Long myChoiceFromCache = null;
            if (isOpen) {
                countsMap = voteCache.getCounts(pollId);
                if (!countsMap.isEmpty()) {
                    myChoiceFromCache = voteCache.getMemberChoice(pollId, memberId);
                }
            }

            var options = p.getOptions().stream()
                    .sorted(java.util.Comparator.comparingInt(o -> o.getSortOrder() == null ? 0 : o.getSortOrder()))
                    .toList();

            java.util.Map<Long, Long> finalCounts;
            Long mySelectedOptionId;

            if (countsMap != null && !countsMap.isEmpty()) {
                finalCounts = countsMap;
                mySelectedOptionId = (myChoiceFromCache != null) ? myChoiceFromCache : myVotes.get(pollId);
            } else {
                finalCounts = new java.util.HashMap<>();
                for (var opt : options) {
                    long cnt = voteRepo.countByPollIdAndOptionId(pollId, opt.getId());
                    finalCounts.put(opt.getId(), cnt);
                }
                mySelectedOptionId = myVotes.get(pollId);

                if (isOpen) {
                    voteCache.warmUp(pollId, finalCounts, memberId, mySelectedOptionId, p.getClosesAt());
                }
            }

            int totalVoters = options.stream()
                    .mapToInt(o -> Math.toIntExact(finalCounts.getOrDefault(o.getId(), 0L)))
                    .sum();

            var optionDtos = options.stream()
                    .map(o -> PollDto.ListItem.OptionResult.builder()
                            .id(o.getId())
                            .content(o.getContent())
                            .votes(Math.toIntExact(finalCounts.getOrDefault(o.getId(), 0L)))
                            .build())
                    .toList();

            result.add(PollDto.ListItem.builder()
                    .id(p.getId())
                    .title(p.getTitle())
                    .description(p.getDescription())
                    .categoryCode(p.getCategory().getCode())
                    .status(p.getStatus().name())
                    .closesAt(p.getClosesAt())
                    .totalVoters(totalVoters)
                    .myVoteOptionId(mySelectedOptionId)
                    .options(optionDtos)
                    .build());
        }

        return result;
    }

    @Transactional
    public void close(Long pollId, Long memberId) {
        Poll poll = pollRepo.findById(pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "poll not found"));
        if (poll.getStatus() == PollStatus.CLOSED) return;

        poll.setStatus(PollStatus.CLOSED);
        poll.setUpdatedAt(OffsetDateTime.now());

        final long pId = pollId;
        // ✅ counts 해시만 즉시 삭제. (member choice는 TTL로 자연 만료)
        runAfterCommit(() -> voteCache.evictOnClose(pId));
    }

    @Transactional
    public void unvote(Long pollId, Long memberId) {
        Poll poll = pollRepo.findById(pollId)
                .orElseThrow(() -> new NotFoundException("poll not found"));
        if (poll.getStatus() != PollStatus.OPEN) throw new ConflictException("poll closed");

        Long myOptionId = voteRepo.findByPoll_IdAndMemberId(pollId, memberId)
                .map(v -> v.getOption().getId())
                .orElse(null);
        if (myOptionId == null) return; // 멱등

        voteRepo.deleteByPollIdAndMemberId(pollId, memberId);

        final long pId = pollId, mId = memberId;
        final long prevOpt = myOptionId;
        final OffsetDateTime closesAt = poll.getClosesAt();
        runAfterCommit(() -> voteCache.unvote(pId, prevOpt, mId, closesAt));
    }

    @Transactional
    public void update(Long pollId, Long memberId, PollDto.UpdateReq req) {
        Poll poll = pollRepo.findById(pollId)
                .orElseThrow(() -> new NotFoundException("poll not found"));

        // 권한: 생성자만
        if (!poll.getCreatedBy().equals(memberId)) {
            throw new ConflictException("no permission");
        }
        if (poll.getStatus() == PollStatus.CLOSED) {
            throw new ConflictException("poll closed");
        }

        // 필드 부분 수정
        if (req.title() != null)       poll.setTitle(req.title().trim());
        if (req.description() != null) poll.setDescription(req.description());
        if (req.closesAt() != null)    poll.setClosesAt(req.closesAt());
        if (req.categoryCode() != null && !req.categoryCode().isBlank()) {
            var cat = categoryRepo.findByCode(req.categoryCode())
                    .orElseThrow(() -> new ConflictException("unknown categoryCode"));
            poll.setCategory(cat);
        }

        // 옵션 변경 가능 여부: 한 표라도 있으면 옵션 수정 금지
        boolean hasAnyVote = voteRepo.countByPollId(pollId) > 0;
        if (req.options() != null) {
            if (hasAnyVote) {
                throw new ConflictException("cannot change options after votes exist");
            }

            // 중복 content 방지
            var contents = new java.util.HashSet<String>();
            for (var o : req.options()) {
                if (o.content() == null || o.content().isBlank()) {
                    throw new ConflictException("option content required");
                }
                String key = o.content().trim().toLowerCase();
                if (!contents.add(key)) throw new ConflictException("duplicate option content");
            }

            // upsert: 전달된 목록으로 “치환”
            // 1) 현재 옵션 맵
            var current = optionRepo.findByPollIdOrderBySortOrderAscIdAsc(pollId)
                    .stream().collect(java.util.stream.Collectors.toMap(PollOption::getId, x -> x));

            // 2) 남길/수정할/새로 추가할 옵션 처리
            var keepIds = new java.util.HashSet<Long>();
            int order = 0;
            OffsetDateTime now = OffsetDateTime.now();
            for (var in : req.options()) {
                if (in.id() != null && current.containsKey(in.id())) {
                    // update
                    var opt = current.get(in.id());
                    opt.setContent(in.content().trim());
                    opt.setSortOrder(in.sortOrder() != null ? in.sortOrder() : order);
                    opt.setUpdatedAt(now);
                    keepIds.add(in.id());
                } else {
                    // insert
                    var newOpt = PollOption.builder()
                            .poll(poll)
                            .content(in.content().trim())
                            .sortOrder(in.sortOrder() != null ? in.sortOrder() : order)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    optionRepo.save(newOpt);
                    // 새로 생성한 ID는 keepIds에 굳이 넣지 않아도 됨 (아래 삭제는 current만 대상으로)
                }
                order++;
            }

            // 3) 전달되지 않은 기존 옵션은 삭제
            for (var entry : current.entrySet()) {
                if (!keepIds.contains(entry.getKey())) {
                    optionRepo.delete(entry.getValue());
                }
            }
        }

        poll.setUpdatedAt(OffsetDateTime.now());
    }

    @Transactional
    public void delete(Long pollId, Long memberId) {
        Poll poll = pollRepo.findById(pollId)
                .orElseThrow(() -> new NotFoundException("poll not found"));

        // 권한: 생성자만
        if (!poll.getCreatedBy().equals(memberId)) {
            throw new ConflictException("no permission");
        }

        // 투표가 존재하면 삭제 금지 (필요시 soft delete로 바꿀 수 있음)
        boolean hasAnyVote = voteRepo.countByPollId(pollId) > 0;
        if (hasAnyVote) throw new ConflictException("cannot delete poll after votes exist");

        // 옵션/투표는 FK cascade 또는 수동 정리
        voteRepo.deleteByPollId(pollId); // 안전하게 정리
        optionRepo.deleteByPollId(pollId);
        pollRepo.delete(poll);
    }

    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    try { task.run(); }
                    catch (Exception e) {
                        log.warn("afterCommit task failed", e);
                    }
                }
            });
        } else {
            // 동기화가 비활성인 경우: 안전하게 동기 실행(테스트/비표준 컨텍스트 대비)
            try { task.run(); } catch (Exception e) { log.warn("task failed (no TX sync)", e); }
        }
    }
}