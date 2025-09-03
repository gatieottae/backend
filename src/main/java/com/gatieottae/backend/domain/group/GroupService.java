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
        // 1) 중복 이름 검사: 존재하면 예외
        if (groupRepository.existsByOwnerIdAndName(ownerId, requestDto.getName())) {
            throw new GroupException(GroupErrorCode.GROUP_NAME_DUPLICATED);
        }

        // 2) 초대 코드/만료 시각 생성
        Instant now = Instant.now();
        String inviteCode = inviteCodeGenerator.generate(12);          // 12자리 대문자+숫자 조합 코드.
        Instant expiresAt = now.plusSeconds(60L * 60 * 24 * 7);        // 7일 유효

        // 3) 엔티티 구성(아직 id 없음)
        Group toSave = Group.builder()
                .name(requestDto.getName())
                .description(requestDto.getDescription())
                .ownerId(ownerId)
                .inviteCode(inviteCode)
                .inviteRotatedAt(now)
                .inviteExpiresAt(expiresAt)
                .build();

        // 4) 저장 → ★반드시 반환값(saved) 사용★ (DB/Mock이 부여한 id 포함)
        Group saved = groupRepository.save(toSave);

        // 5) OWNER 멤버 자동 등록 (saved의 id 사용)
        GroupMember owner = GroupMember.builder()
                .groupId(saved.getId())
                .memberId(ownerId)
                .role(GroupMember.Role.OWNER)
                .build();
        groupMemberRepository.save(owner);

        // 6) 응답 DTO는 반드시 saved 기반으로 생성 (id 포함)
        return GroupResponseDto.builder()
                .id(saved.getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .inviteCode(saved.getInviteCode())
                .inviteExpiresAt(saved.getInviteExpiresAt())
                .build();
    }
}