package com.gatieottae.backend.domain.group;

import com.gatieottae.backend.api.group.dto.GroupRequestDto;
import com.gatieottae.backend.api.group.dto.GroupResponseDto;
import com.gatieottae.backend.common.util.InviteCodeGenerator;
import com.gatieottae.backend.repository.group.GroupMemberRepository;
import com.gatieottae.backend.repository.group.GroupRepository;
import com.gatieottae.backend.domain.group.exception.GroupException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;   // AssertJ - 가독성 좋은 단언문
import static org.mockito.BDDMockito.*;            // Mockito BDD 스타일(given/then/verify)

class GroupServiceTest {

    // ✅ Repository 들은 외부 의존성이므로 "목(mock)"으로 대체한다.
    // 실제 DB에 접근하지 않고, 원하는 리턴값/행동을 지정해서 단위 테스트에만 집중할 수 있다.
    GroupRepository groupRepository = mock(GroupRepository.class);
    GroupMemberRepository groupMemberRepository = mock(GroupMemberRepository.class);
    InviteCodeGenerator inviteCodeGenerator = mock(InviteCodeGenerator.class);  // ✅ 추가


    // ✅ SUT(System Under Test): 테스트 대상 클래스
    // 지금은 Service가 단순히 두 레포지토리만 주입받는다고 가정했다.
    // (만약 InviteCodeGenerator 같은 의존성이 추가되면 여기에도 mock을 만들어 넘겨야 함)
    GroupService sut = new GroupService(groupRepository, groupMemberRepository, inviteCodeGenerator);

    @Test
    @DisplayName("그룹 생성 성공 → 그룹 저장 + OWNER 멤버 등록")
    void createGroup_success() {
        // [Given] 준비 단계: 입력값 & 목 스텁 설정
        Long ownerId = 1L;
        GroupRequestDto req = new GroupRequestDto("제주도 힐링", "설명");

        // 📌 중복명 검사 스텁:
        //   서비스는 먼저 existsByOwnerIdAndName(...)으로 중복 여부를 물어본다.
        //   성공 케이스이므로 false(중복 아님)로 세팅.
        given(groupRepository.existsByOwnerIdAndName(ownerId, req.getName()))
                .willReturn(false);

        // 📌 저장(save) 스텁:
        //   JPA는 save 후 "id"를 채워 반환하지만, 단위 테스트에서는 우리가 직접 결과를 흉내 내야 한다.
        //   willAnswer로 save에 들어온 엔티티를 받아 'id, 초대코드, 만료시각' 등을 채워서 되돌려준다.
        //   여기서 g.toBuilder()를 쓰려면 엔티티에 @Builder(toBuilder=true)가 켜져 있어야 한다.
        given(groupRepository.save(any(Group.class))).willAnswer(inv -> {
            Group g = inv.getArgument(0); // save에 들어온 원본 엔티티
            return g.toBuilder()
                    .id(1L) // ← 마치 DB가 PK를 발급한 것처럼
                    .inviteCode("ABCDEFGH12")
                    .inviteExpiresAt(Instant.now().plusSeconds(3600))
                    .inviteRotatedAt(Instant.now())
                    .build();
        });

        // [When] 실행 단계: SUT 호출
        GroupResponseDto res = sut.createGroup(ownerId, req);

        // [Then] 검증 단계: 기대한 결과/행동이 발생했는지 단언(assert) + 상호작용(verify)
        // ✅ 응답 DTO에 id가 채워져 있어야 한다.
        //    (서비스가 save()의 '반환값(saved)'을 사용해서 DTO를 만들었다는 증거)
        assertThat(res.getId()).isEqualTo(1L);

        // ✅ 이름 매핑이 올바른지 확인
        assertThat(res.getName()).isEqualTo("제주도 힐링");

        // ✅ 저장 로직이 실제로 한 번 호출되었는지 확인
        verify(groupRepository).save(any(Group.class));

        // ✅ OWNER 멤버 자동 등록 로직이 호출되었는지 확인
        verify(groupMemberRepository).save(any(GroupMember.class));
    }

    @Test
    @DisplayName("중복 그룹명 → 예외 발생")
    void createGroup_dupName() {
        // [Given]
        Long ownerId = 1L;
        GroupRequestDto req = new GroupRequestDto("중복", "설명");

        // 📌 중복 케이스: exists... 가 true를 반환하도록 스텁
        given(groupRepository.existsByOwnerIdAndName(ownerId, req.getName())).willReturn(true);

        // [When & Then]
        // ✅ 서비스가 중복 이름인 경우 GroupException을 던지는지 검증
        assertThatThrownBy(() -> sut.createGroup(ownerId, req))
                .isInstanceOf(GroupException.class);

        // ✅ 중복이면 DB에 저장이 일어나면 안 된다.
        verify(groupRepository, never()).save(any());
        verify(groupMemberRepository, never()).save(any());
    }
}