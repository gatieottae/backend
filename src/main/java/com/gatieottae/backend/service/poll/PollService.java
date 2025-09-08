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
        // 옵션이 해당 poll 소속인지 검증 (권장)
        PollOption opt = optionRepo.findById(optionId)
                .orElseThrow(() -> new NotFoundException("option not found"));
        if (!opt.getPoll().getId().equals(pollId)) {
            throw new ConflictException("option not in this poll");
        }

        // ✅ 캐시 정확도를 위해 "기존 내 선택"을 미리 읽어둠
        Long previousOptionId = voteRepo.findByPoll_IdAndMemberId(pollId, memberId)
                .map(v -> v.getOption().getId())
                .orElse(null);

        // DB upsert
        voteRepo.upsertVote(pollId, optionId, memberId);

        // ✅ 트랜잭션 내에서 예외 없이 여기까지 오면 캐시 갱신
        //    - 같은 옵션으로 교체한 경우(previous==new)에도 호출해 두면 안전
        voteCache.applyVote(pollId, optionId, memberId, previousOptionId);
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
        voteCache.warmUp(pollId, counts, memberId, myOptionId);

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

    @Transactional
    public void close(Long pollId, Long memberId) {
        Poll poll = pollRepo.findById(pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "poll not found"));
        if (poll.getStatus() == PollStatus.CLOSED) return;
        // 권한 체크: 생성자만 마감하도록 할지, OWNER/ADMIN만 가능하도록 할지
        poll.setStatus(PollStatus.CLOSED);
        poll.setUpdatedAt(OffsetDateTime.now());

        // 캐시 flush 로직: 현 단계에서는 캐시 삭제만
        try {
            voteCache.evict(pollId);
        } catch (Exception e) {
            log.warn("failed to evict vote cache for poll {}", pollId, e);
            // evict 실패해도 트랜잭션 실패로 만들 필요는 X
        }
    }

    @Transactional(readOnly = true)
    public List<PollDto.ListItem> list(Long groupId, Long memberId) {
        var polls = pollRepo.findByGroupIdOrderByCreatedAtDesc(groupId);
        if (polls.isEmpty()) return List.of();

        // pollIds / optionIds 수집
        var pollIds = polls.stream().map(Poll::getId).toList();
        var allOptions = polls.stream()
                .flatMap(p -> p.getOptions().stream())
                .toList();
        var optionIds = allOptions.stream().map(PollOption::getId).toList();

        // optionId → 득표수 맵
        var counts = new java.util.HashMap<Long, Integer>();
        if (!optionIds.isEmpty()) {
            for (Object[] row : voteRepo.countByOptionIds(optionIds)) {
                Long optionId = (Long) row[0];
                Long cnt = (Long) row[1];
                counts.put(optionId, cnt.intValue());
            }
        }

        // 내 투표 맵 (pollId → optionId)
        var myVotes = new java.util.HashMap<Long, Long>();
        for (var v : voteRepo.findByPoll_IdInAndMemberId(pollIds, memberId)) {
            myVotes.put(v.getPoll().getId(), v.getOption().getId());
        }

        // pollId → 총 투표자 수(사람 수). 단일선택이므로 poll_vote by pollId count
        var totalPerPoll = new java.util.HashMap<Long, Integer>();
        // 간단히 option 합으로 구함
        for (var p : polls) {
            int sum = p.getOptions().stream()
                    .mapToInt(o -> counts.getOrDefault(o.getId(), 0))
                    .sum();
            totalPerPoll.put(p.getId(), sum);
        }

        // DTO 변환
        return polls.stream().map(p -> {
            var optionDtos = p.getOptions().stream()
                    .sorted(java.util.Comparator.comparingInt(o -> o.getSortOrder() == null ? 0 : o.getSortOrder()))
                    .map(o -> PollDto.ListItem.OptionResult.builder()
                            .id(o.getId())
                            .content(o.getContent())
                            .votes(counts.getOrDefault(o.getId(), 0))
                            .build())
                    .toList(); // type: List<PollDto.ListItem.OptionResult>

            return PollDto.ListItem.builder()
                    .id(p.getId())
                    .title(p.getTitle())
                    .description(p.getDescription())
                    .categoryCode(p.getCategory().getCode()) // or getCategoryCode() 쓰는 형태면 변경
                    .status(p.getStatus().name())
                    .closesAt(p.getClosesAt())
                    .totalVoters(totalPerPoll.getOrDefault(p.getId(), 0))
                    .myVoteOptionId(myVotes.get(p.getId()))
                    .options(optionDtos)
                    .build();
        }).toList();
    }

    @Transactional
    public void unvote(Long pollId, Long memberId) {
        // 1) pollRepo에서 Poll을 가져와 상태 체크
        Poll poll = pollRepo.findById(pollId)
                .orElseThrow(() -> new NotFoundException("poll not found"));
        if (poll.getStatus() != PollStatus.OPEN) {
            throw new ConflictException("poll closed");
        }

        // ✅ 내 기존 선택을 확인 (없으면 멱등하게 종료)
        var myVoteOpt = voteRepo.findByPoll_IdAndMemberId(pollId, memberId)
                .map(v -> v.getOption().getId())
                .orElse(null);

        if (myVoteOpt == null) {
            return; // 이미 투표 안 한 상태 → 멱등
        }

        // DB 삭제
        voteRepo.deleteByPollIdAndMemberId(pollId, memberId);

        // ✅ 캐시 감소 반영
        voteCache.unvote(pollId, myVoteOpt, memberId);
    }


    // com.gatieottae.backend.service.poll.PollService

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
}