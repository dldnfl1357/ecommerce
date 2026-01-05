package com.example.ecommerce.domain.seller.repository;

import com.example.ecommerce.domain.seller.entity.Seller;
import com.example.ecommerce.domain.seller.entity.SellerStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface SellerRepository extends ReactiveCrudRepository<Seller, Long> {

    /**
     * 회원 ID로 판매자 조회
     */
    Mono<Seller> findByMemberId(Long memberId);

    /**
     * 사업자등록번호로 판매자 조회
     */
    Mono<Seller> findByBusinessNumber(String businessNumber);

    /**
     * 회원 ID로 판매자 존재 여부 확인
     */
    Mono<Boolean> existsByMemberId(Long memberId);

    /**
     * 사업자등록번호 중복 확인
     */
    Mono<Boolean> existsByBusinessNumber(String businessNumber);

    /**
     * 상태별 판매자 조회
     */
    Flux<Seller> findByStatus(SellerStatus status);

    /**
     * 상태별 판매자 수 조회
     */
    Mono<Long> countByStatus(SellerStatus status);

    /**
     * 활성 판매자 조회 (회원 ID로)
     */
    @Query("SELECT * FROM sellers WHERE member_id = :memberId AND status = 'ACTIVE'")
    Mono<Seller> findActiveByMemberId(Long memberId);
}
