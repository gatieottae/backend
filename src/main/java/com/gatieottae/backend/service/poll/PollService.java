package com.gatieottae.backend.service.poll;

import com.gatieottae.backend.api.poll.dto.PollDto;
import com.gatieottae.backend.common.exception.ConflictException;
import com.gatieottae.backend.common.exception.NotFoundException;
import com.gatieottae.backend.domain.poll.*;
import com.gatieottae.backend.repository.poll.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PollService {

    private final PollRepository pollRepo;
    private final PollCategoryRepository categoryRepo;
    private final PollOptionRepository optionRepo;
    private final PollVoteRepository voteRepo;

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
        voteRepo.upsertVote(pollId, optionId, memberId);
    }

    @Transactional(readOnly = true)
    public PollDto.ResultsRes results(Long pollId, Long memberId) {
        Poll poll = pollRepo.findById(pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "poll not found"));

        var options = optionRepo.findByPollIdOrderBySortOrderAscIdAsc(pollId);
        var myVote = voteRepo.findByPoll_IdAndMemberId(pollId, memberId).orElse(null);

        List<PollDto.ResultsRes.OptionResult> list = new ArrayList<>();
        for (PollOption opt : options) {
            long cnt = voteRepo.countByPollIdAndOptionId(pollId, opt.getId());
            boolean isMine = (myVote != null) && myVote.getOption().getId().equals(opt.getId());
            list.add(new PollDto.ResultsRes.OptionResult(opt.getId(), opt.getContent(), cnt, isMine));
        }

        return new PollDto.ResultsRes(
                poll.getId(),
                poll.getTitle(),
                poll.getCategory().getCode(),
                poll.getStatus().name(),
                poll.getClosesAt(),
                list,
                myVote != null
        );
    }

    @Transactional
    public void close(Long pollId, Long memberId) {
        Poll poll = pollRepo.findById(pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "poll not found"));
        if (poll.getStatus() == PollStatus.CLOSED) return;
        // (선택) 권한 체크: 생성자만 마감하도록 할지, OWNER/ADMIN만 가능하도록 할지
        poll.setStatus(PollStatus.CLOSED);
        poll.setUpdatedAt(OffsetDateTime.now());
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
        for (var v : voteRepo.findByPollIdInAndMemberId(pollIds, memberId)) {
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

        // 2) 내 투표기록 삭제 (없어도 0건 삭제로 끝 — idempotent)
        voteRepo.deleteByPollIdAndMemberId(pollId, memberId);
    }
}