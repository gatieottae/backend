// src/main/java/com/gatieottae/backend/repository/poll/PollOptionRepository.java
package com.gatieottae.backend.repository.poll;

import com.gatieottae.backend.domain.poll.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PollOptionRepository extends JpaRepository<PollOption, Long> {
    List<PollOption> findByPollIdOrderBySortOrderAscIdAsc(Long pollId);

    @Modifying
    @Query("delete from PollOption o where o.poll.id = :pollId")
    void deleteByPollId(@Param("pollId") Long pollId);
}