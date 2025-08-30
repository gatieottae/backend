package com.gatieottae.backend.api;

import com.gatieottae.backend.repository.member.MemberRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberRepository memberRepository;
    public MemberController(MemberRepository memberRepository) { this.memberRepository = memberRepository; }

    @GetMapping("/check")
    public boolean exists(@RequestParam String email) {
        return memberRepository.existsByEmail(email);
    }
}