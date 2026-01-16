package com.example.ecommerce.order.domain.order.repository;

import com.example.ecommerce.order.domain.order.entity.Order;
import com.example.ecommerce.order.domain.order.entity.OrderStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {

    Mono<Order> findByOrderNumber(String orderNumber);

    Flux<Order> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    Flux<Order> findByMemberIdAndStatusOrderByCreatedAtDesc(Long memberId, OrderStatus status);

    @Query("SELECT * FROM orders WHERE member_id = :memberId ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<Order> findByMemberIdWithPagination(Long memberId, int limit, int offset);

    @Query("SELECT COUNT(*) FROM orders WHERE member_id = :memberId")
    Mono<Long> countByMemberId(Long memberId);

    @Query("SELECT * FROM orders WHERE status = :status AND created_at < :deadline")
    Flux<Order> findPendingOrdersBeforeDeadline(OrderStatus status, java.time.LocalDateTime deadline);
}
