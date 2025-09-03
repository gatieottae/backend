package com.gatieottae.backend.domain.group;

import com.gatieottae.backend.api.group.dto.GroupRequestDto;
import com.gatieottae.backend.api.group.dto.GroupResponseDto;
import com.gatieottae.backend.common.util.InviteCodeGenerator;
import com.gatieottae.backend.domain.group.exception.GroupErrorCode;
import com.gatieottae.backend.domain.group.exception.GroupException;
import com.gatieottae.backend.repository.group.GroupMemberRepository;
import com.gatieottae.backend.repository.group.GroupRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 그룹 관련 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    /**
     * 새로운 그룹 생성
     * - 동일 owner 내 중복 이름 방지
     * - 초대코드 발급
     * - OWNER 멤버 자동 등록
     */
    @Transactional
    public GroupResponseDto createGroup(Long ownerId, GroupRequestDto requestDto) {
        // 1. 중복 이름 검사
        if (groupRepository.existsByOwnerIdAndName(ownerId, requestDto.getName())) {
            throw new GroupException(GroupErrorCode.GROUP_NAME_DUPLICATED);
        }

        // 2. 그룹 생성
        String inviteCode = InviteCodeGenerator.generateDefault();
        Instant expiresAt = Instant.now().plusSeconds(60 * 60 * 24 * 7); // 7일 유효

        Group group = Group.builder()
                .name(requestDto.getName())
                .description(requestDto.getDescription())
                .ownerId(ownerId)
                .inviteCode(inviteCode)
                .inviteExpiresAt(expiresAt)
                .inviteRotatedAt(Instant.now())
                .build();

        groupRepository.save(group);

        // 3. OWNER 멤버 자동 등록
        GroupMember groupMember = GroupMember.builder()
                .groupId(group.getId())
                .memberId(ownerId)
                .role(GroupMember.Role.OWNER)
                .build();

        groupMemberRepository.save(groupMember);

        // 4. 응답 DTO 반환
        return GroupResponseDto.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .inviteCode(group.getInviteCode())
                .inviteExpiresAt(group.getInviteExpiresAt())
                .build();
    }
}