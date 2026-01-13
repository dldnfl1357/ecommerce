package com.example.ecommerce.domain.member.repository;

import com.example.ecommerce.domain.member.entity.Address;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface AddressRepository extends ReactiveCrudRepository<Address, Long> {

    /**
     * 회원의 배송지 목록 조회
     */
    Flux<Address> findByMemberId(Long memberId);

    /**
     * 회원의 기본 배송지 조회
     */
    Mono<Address> findByMemberIdAndIsDefaultTrue(Long memberId);

    /**
     * 회원의 배송지 개수 조회
     */
    Mono<Long> countByMemberId(Long memberId);

    /**
     * 회원의 모든 배송지 기본 설정 해제
     */
    @Modifying
    @Query("UPDATE addresses SET is_default = false WHERE member_id = :memberId AND is_default = true")
    Mono<Integer> unsetDefaultByMemberId(@Param("memberId") Long memberId);

    /**
     * 회원의 모든 배송지 삭제
     */
    Mono<Void> deleteByMemberId(Long memberId);
}
