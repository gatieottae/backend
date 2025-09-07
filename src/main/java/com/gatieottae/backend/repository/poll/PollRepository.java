package com.gatieottae.backend.repository.poll;

import com.gatieottae.backend.domain.poll.Poll;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PollRepository extends JpaRepository<Poll, Long> {
    List<Poll> findByGroupIdOrderByCreatedAtDesc(Long groupId);
}