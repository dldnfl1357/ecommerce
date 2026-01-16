package com.example.ecommerce.order.domain.delivery.repository;

import com.example.ecommerce.order.domain.delivery.entity.Delivery;
import com.example.ecommerce.order.domain.delivery.entity.DeliveryStatus;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DeliveryRepository extends ReactiveCrudRepository<Delivery, Long> {

    Mono<Delivery> findByOrderId(Long orderId);

    Mono<Delivery> findByTrackingNumber(String trackingNumber);

    Flux<Delivery> findByStatus(DeliveryStatus status);
}
