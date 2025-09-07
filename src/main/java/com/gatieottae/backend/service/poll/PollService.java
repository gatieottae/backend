package com.gatieottae.backend.service.poll;

import com.gatieottae.backend.api.poll.dto.PollDto;
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "poll not found"));
        if (poll.getStatus() == PollStatus.CLOSED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "poll is closed");
        if (poll.getClosesAt() != null && poll.getClosesAt().isBefore(OffsetDateTime.now()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "poll is already expired");

        // 중복 투표 방지 (DB unique도 있지만 사전 체크)
        voteRepo.findByPoll_IdAndMemberId(pollId, memberId).ifPresent(v -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "already voted");
        });

        // 옵션 검증
        PollOption option = optionRepo.findById(optionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "option not found"));
        if (!option.getPoll().getId().equals(pollId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "option does not belong to poll");

        PollVote vote = PollVote.builder()
                .poll(poll)
                .option(option)
                .memberId(memberId)
                .votedAt(OffsetDateTime.now())
                .build();
        voteRepo.save(vote);
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

}