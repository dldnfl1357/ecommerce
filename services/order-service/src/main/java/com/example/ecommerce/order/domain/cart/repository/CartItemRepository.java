package com.example.ecommerce.order.domain.cart.repository;

import com.example.ecommerce.order.domain.cart.entity.CartItem;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CartItemRepository extends ReactiveCrudRepository<CartItem, Long> {

    Flux<CartItem> findByCartId(Long cartId);

    Flux<CartItem> findByCartIdAndIsSelectedTrue(Long cartId);

    Mono<CartItem> findByCartIdAndProductOptionId(Long cartId, Long productOptionId);

    @Modifying
    @Query("DELETE FROM cart_items WHERE cart_id = :cartId")
    Mono<Void> deleteByCartId(Long cartId);

    @Modifying
    @Query("DELETE FROM cart_items WHERE cart_id = :cartId AND is_selected = true")
    Mono<Void> deleteSelectedByCartId(Long cartId);

    @Query("SELECT COUNT(*) FROM cart_items WHERE cart_id = :cartId")
    Mono<Long> countByCartId(Long cartId);
}
