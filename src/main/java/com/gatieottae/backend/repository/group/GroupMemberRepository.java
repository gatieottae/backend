package com.gatieottae.backend.repository.group;

import com.gatieottae.backend.domain.group.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    /**
     * 특정 그룹에 이미 가입되어 있는지 검사 (참여 API 멱등성 확보).
     */
    boolean existsByGroupIdAndMemberId(Long groupId, Long memberId);

    List<GroupMember> findAllByGroupId(Long groupId);

    // 그룹 ID로 멤버 ID만 뽑아오기
    @Query("select gm.memberId from GroupMember gm where gm.groupId = :groupId")
    List<Long> findMemberIdsByGroupId(@Param("groupId") Long groupId);
}