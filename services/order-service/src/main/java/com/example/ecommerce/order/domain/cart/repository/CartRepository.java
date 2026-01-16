package com.example.ecommerce.order.domain.cart.repository;

import com.example.ecommerce.order.domain.cart.entity.Cart;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface CartRepository extends ReactiveCrudRepository<Cart, Long> {

    Mono<Cart> findByMemberId(Long memberId);
}
