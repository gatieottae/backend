package com.gatieottae.backend.repository.group;

import com.gatieottae.backend.domain.group.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {

    /**
     * 동일 소유자(ownerId) 내 동일 이름(name)의 그룹 존재 여부.
     * - 그룹 생성 시 중복 방지에 사용.
     */
    boolean existsByOwnerIdAndName(Long ownerId, String name);

    /**
     * 초대코드로 그룹 조회 (참여/유효성 검사에 사용).
     */
    Optional<Group> findByInviteCode(String inviteCode);
}