package com.gatieottae.backend.service.auth;

import com.gatieottae.backend.domain.member.Member;
import com.gatieottae.backend.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeService {
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public Member getMeByUsername(String username) {
        return memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("회원 정보를 찾을 수 없습니다. username=" + username));
    }

    @Transactional
    public Member updateName(String username, String newName) {
        String trimmed = newName == null ? "" : newName.trim();
        if (trimmed.isEmpty() || trimmed.length() > 100) {
            throw new IllegalArgumentException("INVALID_NAME");
        }
        Member m = getMeByUsername(username);
        m.changeName(trimmed); // JPA 변경 감지
        return m;
    }
}