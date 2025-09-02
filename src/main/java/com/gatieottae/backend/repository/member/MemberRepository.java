package com.gatieottae.backend.repository.member;

import com.gatieottae.backend.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Member 엔티티용 JPA Repository
 * - 기본 CRUD는 JpaRepository에서 제공
 * - 추가 조회 메서드 정의
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    /** username 존재 여부 확인 */
    boolean existsByUsername(String username);

    /** email 존재 여부 확인 (nullable 고려) */
    boolean existsByEmail(String email);

    /** username으로 Member 조회 */
    Optional<Member> findByUsername(String username);

    /** email로 Member 조회 */
    Optional<Member> findByEmail(String email);

    /** 소셜 전용: provider + subject 조합 조회 (DB에 유니크 인덱스 존재) */
    Optional<Member> findByOauthProviderAndOauthSubject(String oauthProvider, String oauthSubject);
}