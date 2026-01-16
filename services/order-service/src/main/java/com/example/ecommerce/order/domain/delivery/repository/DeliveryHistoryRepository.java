package com.example.ecommerce.order.domain.delivery.repository;

import com.example.ecommerce.order.domain.delivery.entity.DeliveryHistory;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface DeliveryHistoryRepository extends ReactiveCrudRepository<DeliveryHistory, Long> {

    Flux<DeliveryHistory> findByDeliveryIdOrderByOccurredAtDesc(Long deliveryId);
}
