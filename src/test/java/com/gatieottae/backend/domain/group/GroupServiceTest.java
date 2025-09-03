package com.gatieottae.backend.domain.group;

import com.gatieottae.backend.api.group.dto.GroupRequestDto;
import com.gatieottae.backend.api.group.dto.GroupResponseDto;
import com.gatieottae.backend.common.util.InviteCodeGenerator; // static 모킹 대상
import com.gatieottae.backend.domain.group.exception.GroupException;
import com.gatieottae.backend.repository.group.GroupMemberRepository;
import com.gatieottae.backend.repository.group.GroupRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock GroupRepository groupRepository;
    @Mock GroupMemberRepository groupMemberRepository;

    // ⚠️ 현재 Service가 InviteCodeGenerator를 주입받는 형태라면 @InjectMocks 생성이 실패할 수 있어요.
    //     - Service가 실제로 "static 메서드"를 호출(InviteCodeGenerator.generate(...))하고
    //       필드 주입값을 쓰지 않는다면, 기본 생성자 혹은 필드 주입 없이도 돌아갑니다.
    //     - 만약 생성자에서 꼭 필요하면 테스트에서 리플렉션으로 주입하거나,
    //       Service 생성자를 (repo 2개만 받는) 오버로드로 하나 더 만드는 게 깔끔합니다.
    @InjectMocks GroupService sut;

    @Test
    @DisplayName("그룹 생성 성공 → 저장 + OWNER 등록 + 초대코드 세팅")
    void create_success() {
        Long ownerId = 1L;
        GroupRequestDto req = new GroupRequestDto(
                "제주도 힐링", "봄 여행",
                "제주도",
                LocalDate.of(2025,3,1),
                LocalDate.of(2025,3,3)
        );

        given(groupRepository.existsByOwnerIdAndName(ownerId, req.getName())).willReturn(false);
        // save가 반환하는 엔티티에 id가 채워지도록 흉내
        given(groupRepository.save(any(Group.class))).willAnswer(inv -> {
            Group g = inv.getArgument(0);
            return g.toBuilder().id(100L).build();
        });

        // ⭐ static 메서드 모킹
        try (MockedStatic<InviteCodeGenerator> mocked = org.mockito.Mockito.mockStatic(InviteCodeGenerator.class)) {
            mocked.when(() -> InviteCodeGenerator.generate(12)).thenReturn("CODE12345678");

            GroupResponseDto res = sut.createGroup(ownerId, req);

            assertThat(res.getId()).isEqualTo(100L);
            assertThat(res.getName()).isEqualTo("제주도 힐링");
            assertThat(res.getInviteCode()).isEqualTo("CODE12345678");

            InOrder io = inOrder(groupRepository, groupMemberRepository);
            io.verify(groupRepository).existsByOwnerIdAndName(ownerId, "제주도 힐링");
            io.verify(groupRepository).save(any(Group.class));
            io.verify(groupMemberRepository).save(any(GroupMember.class));
            io.verifyNoMoreInteractions();

            // static 호출 검증(선택)
            mocked.verify(() -> InviteCodeGenerator.generate(12), times(1));
        }
    }

    @Test
    @DisplayName("동일 owner 내 이름 중복이면 예외")
    void duplicate_same_owner() {
        Long ownerId = 1L;
        GroupRequestDto req = new GroupRequestDto("중복", "desc", "부산", null, null);
        given(groupRepository.existsByOwnerIdAndName(ownerId, "중복")).willReturn(true);

        assertThatThrownBy(() -> {
            try (MockedStatic<InviteCodeGenerator> ignored = org.mockito.Mockito.mockStatic(InviteCodeGenerator.class)) {
                sut.createGroup(ownerId, req);
            }
        }).isInstanceOf(GroupException.class);

        // save / member 등록 호출 안 됨
        then(groupRepository).shouldHaveNoMoreInteractions();
        then(groupMemberRepository).shouldHaveNoInteractions();
        // static 모킹은 verify 하지 않아도 됨(어차피 예외로 흐름 종료)
    }

    @Test
    @DisplayName("다른 owner는 같은 이름 허용")
    void same_name_different_owner_allowed() {
        Long ownerA = 1L, ownerB = 2L;
        GroupRequestDto req = new GroupRequestDto("같은이름", "desc", "속초", null, null);

        given(groupRepository.existsByOwnerIdAndName(ownerA, "같은이름")).willReturn(false);
        given(groupRepository.existsByOwnerIdAndName(ownerB, "같은이름")).willReturn(false);
        given(groupRepository.save(any(Group.class))).willAnswer(inv -> {
            Group g = inv.getArgument(0);
            long id = g.getOwnerId().equals(ownerA) ? 1L : 2L;
            return g.toBuilder().id(id).build();
        });

        try (MockedStatic<InviteCodeGenerator> mocked = org.mockito.Mockito.mockStatic(InviteCodeGenerator.class)) {
            // 두 번 호출 → 서로 다른 값 리턴
            mocked.when(() -> InviteCodeGenerator.generate(12))
                    .thenReturn("CODE_A", "CODE_B");

            GroupResponseDto a = sut.createGroup(ownerA, req);
            GroupResponseDto b = sut.createGroup(ownerB, req);

            assertThat(a.getId()).isEqualTo(1L);
            assertThat(b.getId()).isEqualTo(2L);

            then(groupRepository).should(times(1)).existsByOwnerIdAndName(ownerA, "같은이름");
            then(groupRepository).should(times(1)).existsByOwnerIdAndName(ownerB, "같은이름");
            then(groupRepository).should(times(2)).save(any(Group.class));
            then(groupMemberRepository).should(times(2)).save(any(GroupMember.class));

            mocked.verify(() -> InviteCodeGenerator.generate(12), times(2));
        }
    }
}