package com.example.ecommerce.product.domain.seller.repository;

import com.example.ecommerce.product.domain.seller.entity.Seller;
import com.example.ecommerce.product.domain.seller.entity.SellerStatus;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface SellerRepository extends ReactiveCrudRepository<Seller, Long> {

    Mono<Seller> findByMemberId(Long memberId);

    Mono<Seller> findByBusinessNumber(String businessNumber);

    Mono<Boolean> existsByMemberId(Long memberId);

    Mono<Boolean> existsByBusinessNumber(String businessNumber);

    Flux<Seller> findByStatus(SellerStatus status);

    Mono<Long> countByStatus(SellerStatus status);
}
