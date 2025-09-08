// src/main/java/com/gatieottae/backend/repository/poll/PollCategoryRepository.java
package com.gatieottae.backend.repository.poll;

import com.gatieottae.backend.domain.poll.PollCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PollCategoryRepository extends JpaRepository<PollCategory, Long> {
    Optional<PollCategory> findByCode(String code);
}