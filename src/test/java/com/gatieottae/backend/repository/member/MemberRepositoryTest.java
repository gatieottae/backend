package com.gatieottae.backend.repository.member;

import com.gatieottae.backend.domain.member.Member;
import com.gatieottae.backend.domain.member.MemberStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest // JPA 관련 슬라이스 테스트 (내장 DB H2 사용)
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("회원 저장 및 조회 테스트")
    void saveAndFindMember() {
        // given
        Member member = Member.builder()
                .username("alice01")
                .passwordHash("hashed-password")
                .email("alice@example.com")
                .name("Alice")
                .status(MemberStatus.ACTIVE)
                .build();

        // when
        Member saved = memberRepository.save(member);

        // then
        assertThat(saved.getId()).isNotNull();

        Optional<Member> found = memberRepository.findByUsername("alice01");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("username 중복 여부 확인")
    void existsByUsername() {
        // given
        Member member = Member.builder()
                .username("bob01")
                .passwordHash("hashed-password")
                .name("Bob")
                .build();
        memberRepository.save(member);

        // when
        boolean exists = memberRepository.existsByUsername("bob01");

        // then
        assertThat(exists).isTrue();
    }
}