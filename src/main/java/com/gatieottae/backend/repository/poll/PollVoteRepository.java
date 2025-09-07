package com.gatieottae.backend.repository.poll;

import com.gatieottae.backend.domain.poll.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {

    Optional<PollVote> findByPoll_IdAndMemberId(Long pollId, Long memberId);

    long countByPollIdAndOptionId(Long pollId, Long id);

    // optionId별 득표수
    @Query("select v.option.id as optionId, count(v.id) as cnt " +
            "from PollVote v where v.option.id in :optionIds group by v.option.id")
    List<Object[]> countByOptionIds(@Param("optionIds") List<Long> optionIds);

    // 내 투표(여러 poll에 대해 한 번에)
    List<PollVote> findByPollIdInAndMemberId(List<Long> pollIds, Long memberId);

    void deleteByPollIdAndMemberId(Long pollId, Long memberId);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO gatieottae.poll_vote (poll_id, option_id, member_id, voted_at)
        VALUES (:pollId, :optionId, :memberId, NOW())
        ON CONFLICT (poll_id, member_id)
        DO UPDATE SET option_id = EXCLUDED.option_id, voted_at = NOW()
    """, nativeQuery = true)
    void upsertVote(@Param("pollId") Long pollId,
                    @Param("optionId") Long optionId,
                    @Param("memberId") Long memberId);

}