package com.example.ecommerce.member.domain.address.repository;

import com.example.ecommerce.member.domain.address.entity.Address;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface AddressRepository extends ReactiveCrudRepository<Address, Long> {

    Flux<Address> findByMemberIdOrderByIsDefaultDescCreatedAtDesc(Long memberId);

    Mono<Address> findByMemberIdAndIsDefaultTrue(Long memberId);

    Mono<Long> countByMemberId(Long memberId);

    Mono<Void> deleteByMemberId(Long memberId);
}
