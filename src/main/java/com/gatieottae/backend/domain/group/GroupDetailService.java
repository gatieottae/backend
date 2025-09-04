package com.gatieottae.backend.domain.group;

import com.gatieottae.backend.api.group.dto.GroupDetailResponseDto;
import com.gatieottae.backend.common.exception.NotFoundException;
import com.gatieottae.backend.domain.member.Member;
import com.gatieottae.backend.repository.group.GroupMemberRepository;
import com.gatieottae.backend.repository.group.GroupRepository;
import com.gatieottae.backend.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupDetailService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final MemberRepository memberRepository;

    public GroupDetailResponseDto getGroupDetail(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        // 1) 그룹 멤버 로드 (memberId, role만 들어있음)
        List<GroupMember> gms = groupMemberRepository.findAllByGroupId(groupId);

        // 2) memberId → Member 벌크 조회 후 맵핑
        List<Long> memberIds = gms.stream().map(GroupMember::getMemberId).toList();
        Map<Long, Member> membersById = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, m -> m));

        // 3) 정렬: OWNER 우선 → displayName 알파벳 → id 오름차순
        Comparator<GroupMember> cmp = Comparator
                .comparing((GroupMember gm) -> gm.getRole() == GroupMember.Role.OWNER ? 0 : 1)
                .thenComparing(gm -> {
                    Member m = membersById.get(gm.getMemberId());
                    String dn = getDisplayName(m);
                    return dn == null ? "" : dn.toLowerCase();
                })
                .thenComparing(GroupMember::getMemberId);

        List<GroupDetailResponseDto.MemberDto> members = gms.stream()
                .sorted(cmp)
                .map(gm -> {
                    Member m = membersById.get(gm.getMemberId());
                    return GroupDetailResponseDto.MemberDto.builder()
                            .id(m.getId())
                            .displayName(getDisplayName(m))   // nickname 우선, 없으면 name
                            .role(gm.getRole().name())
                            .build();
                })
                .toList();

        return GroupDetailResponseDto.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .destination(group.getDestination())
                .startDate(group.getStartDate())
                .endDate(group.getEndDate())
                .ownerId(group.getOwnerId())
                .memberCount(members.size())
                .members(members)
                .build();
    }

    private String getDisplayName(Member m) {
        String nick = m.getNickname();
        return (nick != null && !nick.isBlank()) ? nick : m.getName();
    }
}