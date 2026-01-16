package com.example.ecommerce.product.domain.inventory.service;

import com.example.ecommerce.common.exception.BusinessException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.common.kafka.Topics;
import com.example.ecommerce.common.kafka.publisher.EventPublisher;
import com.example.ecommerce.events.inventory.StockReleasedEvent;
import com.example.ecommerce.events.inventory.StockReservedEvent;
import com.example.ecommerce.product.domain.inventory.dto.request.InventoryAdjustRequest;
import com.example.ecommerce.product.domain.inventory.dto.request.InventoryReserveRequest;
import com.example.ecommerce.product.domain.inventory.dto.response.InventoryResponse;
import com.example.ecommerce.product.domain.inventory.entity.Inventory;
import com.example.ecommerce.product.domain.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final EventPublisher eventPublisher;

    public Mono<InventoryResponse> getInventory(Long productOptionId) {
        return inventoryRepository.findByProductOptionId(productOptionId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVENTORY_NOT_FOUND)))
                .map(InventoryResponse::from);
    }

    public Flux<InventoryResponse> getLowStockInventory() {
        return inventoryRepository.findLowStockInventory()
                .map(InventoryResponse::from);
    }

    @Transactional
    public Mono<InventoryResponse> reserveStock(InventoryReserveRequest request) {
        String reservationId = UUID.randomUUID().toString();

        return inventoryRepository.findByProductOptionId(request.getProductOptionId())
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVENTORY_NOT_FOUND)))
                .flatMap(inventory -> {
                    if (inventory.getAvailableQuantity() < request.getQuantity()) {
                        return Mono.error(new BusinessException(ErrorCode.INSUFFICIENT_STOCK));
                    }
                    return inventoryRepository.save(inventory.reserve(request.getQuantity()));
                })
                .flatMap(inventory -> {
                    StockReservedEvent event = StockReservedEvent.of(
                            request.getProductOptionId(),
                            request.getOrderId(),
                            request.getQuantity(),
                            reservationId
                    );
                    return eventPublisher.publish(Topics.INVENTORY_EVENTS, event)
                            .thenReturn(inventory);
                })
                .map(InventoryResponse::from)
                .doOnSuccess(response -> log.info("재고 예약 완료: optionId={}, quantity={}",
                        request.getProductOptionId(), request.getQuantity()));
    }

    @Transactional
    public Mono<InventoryResponse> releaseStock(Long productOptionId, Integer quantity, Long orderId, String reason) {
        String reservationId = UUID.randomUUID().toString();

        return inventoryRepository.findByProductOptionId(productOptionId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVENTORY_NOT_FOUND)))
                .map(inventory -> inventory.releaseReservation(quantity))
                .flatMap(inventoryRepository::save)
                .flatMap(inventory -> {
                    StockReleasedEvent event = StockReleasedEvent.of(
                            productOptionId,
                            orderId,
                            quantity,
                            reservationId,
                            reason
                    );
                    return eventPublisher.publish(Topics.INVENTORY_EVENTS, event)
                            .thenReturn(inventory);
                })
                .map(InventoryResponse::from)
                .doOnSuccess(response -> log.info("재고 해제 완료: optionId={}, quantity={}",
                        productOptionId, quantity));
    }

    @Transactional
    public Mono<InventoryResponse> increaseStock(Long productOptionId, InventoryAdjustRequest request) {
        return inventoryRepository.findByProductOptionId(productOptionId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVENTORY_NOT_FOUND)))
                .map(inventory -> inventory.increase(request.getQuantity()))
                .flatMap(inventoryRepository::save)
                .map(InventoryResponse::from)
                .doOnSuccess(response -> log.info("재고 증가 완료: optionId={}, quantity={}",
                        productOptionId, request.getQuantity()));
    }

    @Transactional
    public Mono<InventoryResponse> decreaseStock(Long productOptionId, InventoryAdjustRequest request) {
        return inventoryRepository.findByProductOptionId(productOptionId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVENTORY_NOT_FOUND)))
                .flatMap(inventory -> {
                    if (inventory.getQuantity() < request.getQuantity()) {
                        return Mono.error(new BusinessException(ErrorCode.INSUFFICIENT_STOCK));
                    }
                    return Mono.just(inventory.decrease(request.getQuantity()));
                })
                .flatMap(inventoryRepository::save)
                .map(InventoryResponse::from)
                .doOnSuccess(response -> log.info("재고 감소 완료: optionId={}, quantity={}",
                        productOptionId, request.getQuantity()));
    }
}
