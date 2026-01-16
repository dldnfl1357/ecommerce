package com.example.ecommerce.order.domain.order.repository;

import com.example.ecommerce.order.domain.order.entity.OrderItem;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface OrderItemRepository extends ReactiveCrudRepository<OrderItem, Long> {

    Flux<OrderItem> findByOrderId(Long orderId);

    Flux<OrderItem> findBySellerId(Long sellerId);
}
