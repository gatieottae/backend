package com.gatieottae.backend.repository.poll;

import com.gatieottae.backend.domain.poll.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {

    boolean existsByPoll_IdAndMemberId(Long pollId, Long memberId);

    Optional<PollVote> findByPoll_IdAndMemberId(Long pollId, Long memberId);

    long countByPollIdAndOptionId(Long pollId, Long id);
}