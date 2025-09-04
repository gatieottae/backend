package com.gatieottae.backend.repository.group;

import com.gatieottae.backend.domain.group.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    /**
     * 특정 그룹에 이미 가입되어 있는지 검사 (참여 API 멱등성 확보).
     */
    boolean existsByGroupIdAndMemberId(Long groupId, Long memberId);

    List<GroupMember> findAllByGroupId(Long groupId);
    List<GroupMember> findByGroupId(Long groupId);
}