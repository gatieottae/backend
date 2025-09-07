// src/main/java/com/gatieottae/backend/repository/poll/PollRepository.java
package com.gatieottae.backend.repository.poll;

import com.gatieottae.backend.domain.poll.Poll;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PollRepository extends JpaRepository<Poll, Long> {
}