package com.gatieottae.backend.domain.group;

import com.gatieottae.backend.api.group.dto.GroupRequestDto;
import com.gatieottae.backend.api.group.dto.GroupResponseDto;
import com.gatieottae.backend.common.util.InviteCodeGenerator;
import com.gatieottae.backend.domain.group.exception.GroupErrorCode;
import com.gatieottae.backend.domain.group.exception.GroupException;
import com.gatieottae.backend.repository.group.GroupMemberRepository;
import com.gatieottae.backend.repository.group.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 스프링 트랜잭션
import java.time.Instant;

/**
 * 그룹 관련 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final InviteCodeGenerator inviteCodeGenerator;

    /**
     * 새로운 그룹 생성
     * - 동일 owner 내 중복 이름 방지
     * - 초대코드 발급
     * - OWNER 멤버 자동 등록
     */
    @Transactional
    public GroupResponseDto createGroup(Long ownerId, GroupRequestDto requestDto) {
        if (groupRepository.existsByOwnerIdAndName(ownerId, requestDto.getName())) {
            throw new GroupException(GroupErrorCode.GROUP_NAME_DUPLICATED);
        }

        // 초대코드만 생성 (만료/회전 없음)
        String inviteCode = inviteCodeGenerator.generate(12);

        Group toSave = Group.builder()
                .name(requestDto.getName())
                .description(requestDto.getDescription())
                .ownerId(ownerId)
                .destination(requestDto.getDestination())
                .startDate(requestDto.getStartDate())
                .endDate(requestDto.getEndDate())
                .inviteCode(inviteCode)
                .build();

        Group saved = groupRepository.save(toSave);

        GroupMember owner = GroupMember.builder()
                .groupId(saved.getId())
                .memberId(ownerId)
                .role(GroupMember.Role.OWNER)
                .build();
        groupMemberRepository.save(owner);

        return GroupResponseDto.builder()
                .id(saved.getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .destination(saved.getDestination())
                .startDate(saved.getStartDate())
                .endDate(saved.getEndDate())
                .inviteCode(saved.getInviteCode())
                .build();
    }

    /** 초대코드로 참여 */
    @Transactional
    public GroupResponseDto joinByCode(String code, Long userId) {
        Group group = groupRepository.findByInviteCode(code)
                .orElseThrow(() -> new GroupException(GroupErrorCode.INVALID_CODE));

        if (groupMemberRepository.existsByGroupIdAndMemberId(group.getId(), userId)) {
            throw new GroupException(GroupErrorCode.ALREADY_MEMBER);
        }

        groupMemberRepository.save(GroupMember.create(group, userId, GroupMember.Role.MEMBER));
        return GroupResponseDto.from(group);
    }
}