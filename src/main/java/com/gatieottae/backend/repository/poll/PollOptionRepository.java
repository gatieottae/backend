// src/main/java/com/gatieottae/backend/repository/poll/PollOptionRepository.java
package com.gatieottae.backend.repository.poll;

import com.gatieottae.backend.domain.poll.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PollOptionRepository extends JpaRepository<PollOption, Long> {
    List<PollOption> findByPollIdOrderBySortOrderAscIdAsc(Long pollId);
}