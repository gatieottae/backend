package com.gatieottae.backend.repository.poll;

import com.gatieottae.backend.domain.poll.Poll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;

public interface PollRepository extends JpaRepository<Poll, Long> {
    List<Poll> findByGroupIdOrderByCreatedAtDesc(Long groupId);

    // 상태가 OPEN 이고 closesAt이 now 이전/같은 것들의 ID 페이징 조회
    @Query("""
           select p.id
             from Poll p
            where p.status = com.gatieottae.backend.domain.poll.PollStatus.OPEN
              and p.closesAt is not null
              and p.closesAt <= :now
           order by p.id asc
           """)
    List<Long> findIdsToClose(OffsetDateTime now, Pageable pageable);
}