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

    // 단건 조회
    Optional<PollVote> findByPoll_IdAndMemberId(Long pollId, Long memberId);

    // ✅ 득표수 (연관경로)
    long countByPoll_IdAndOption_Id(Long pollId, Long optionId);

    // ✅ 여러 poll에 대한 내 투표
    List<PollVote> findByPoll_IdInAndMemberId(List<Long> pollIds, Long memberId);

    void deleteByPoll_IdAndMemberId(Long pollId, Long memberId);

    // 집계는 네이티브/JPQL 사용 – 그대로 OK
    @Query("select v.option.id as optionId, count(v.id) as cnt " +
            "from PollVote v where v.option.id in :optionIds group by v.option.id")
    List<Object[]> countByOptionIds(@Param("optionIds") List<Long> optionIds);

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


    long countByPollId(Long pollId);
    void deleteByPollId(Long pollId);
}