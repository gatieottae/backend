package com.gatieottae.backend.repository.member;

import com.gatieottae.backend.domain.member.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MemberRepositoryTest {

    @Autowired
    MemberRepository memberRepository;

    @Test
    @DisplayName("Member 저장/조회가 된다")
    void saveAndFind() {
        Member m = new Member("alice@example.com", "Alice");
        Member saved = memberRepository.save(m);

        assertThat(saved.getId()).isNotNull();
        assertThat(memberRepository.findByEmail("alice@example.com")).isPresent();
    }
}