package com.example.ecommerce.domain.member.repository;

import com.example.ecommerce.domain.member.entity.Member;
import com.example.ecommerce.domain.member.entity.MemberStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface MemberRepository extends ReactiveCrudRepository<Member, Long> {

    /**
     * 이메일로 회원 조회
     */
    Mono<Member> findByEmail(String email);

    /**
     * 전화번호로 회원 조회
     */
    Mono<Member> findByPhone(String phone);

    /**
     * 이메일 중복 체크
     */
    Mono<Boolean> existsByEmail(String email);

    /**
     * 전화번호 중복 체크
     */
    Mono<Boolean> existsByPhone(String phone);

    /**
     * 휴면 회원 조회 (1년 이상 미접속)
     */
    @Query("""
        SELECT * FROM members
        WHERE status = :status
        AND last_login_at < :dormantDate
        """)
    Flux<Member> findDormantMembers(
        @Param("status") MemberStatus status,
        @Param("dormantDate") LocalDateTime dormantDate
    );

    /**
     * 활성 회원 수 조회
     */
    Mono<Long> countByStatus(MemberStatus status);

    /**
     * 이메일과 상태로 회원 조회
     */
    Mono<Member> findByEmailAndStatus(String email, MemberStatus status);

    /**
     * 로켓와우 활성 회원 조회
     */
    @Query("""
        SELECT * FROM members
        WHERE rocket_wow_active = true
        AND rocket_wow_expires_at > :now
        """)
    Flux<Member> findActiveRocketWowMembers(@Param("now") LocalDateTime now);

    /**
     * 등급별 회원 수 조회 (통계)
     */
    @Query("""
        SELECT grade, COUNT(*) as count
        FROM members
        WHERE status = 'ACTIVE'
        GROUP BY grade
        """)
    Flux<Object[]> countMembersByGrade();
}
