package com.example.ecommerce.domain.inventory.service;

import com.example.ecommerce.domain.inventory.dto.request.*;
import com.example.ecommerce.domain.inventory.dto.response.InventoryHistoryResponse;
import com.example.ecommerce.domain.inventory.dto.response.InventoryResponse;
import com.example.ecommerce.domain.inventory.dto.response.InventoryStockResponse;
import com.example.ecommerce.domain.inventory.entity.Inventory;
import com.example.ecommerce.domain.inventory.entity.InventoryHistory;
import com.example.ecommerce.domain.inventory.repository.InventoryHistoryRepository;
import com.example.ecommerce.domain.inventory.repository.InventoryRepository;
import com.example.ecommerce.domain.product.repository.ProductOptionRepository;
import com.example.ecommerce.global.common.PageResponse;
import com.example.ecommerce.global.exception.BusinessException;
import com.example.ecommerce.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryHistoryRepository historyRepository;
    private final ProductOptionRepository productOptionRepository;

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final Duration RETRY_DELAY = Duration.ofMillis(100);

    // ========== 재고 생성 ==========

    /**
     * 재고 생성 (상품 옵션 등록 시)
     */
    @Transactional
    public Mono<InventoryResponse> createInventory(InventoryCreateRequest request) {
        return validateProductOptionExists(request.getProductOptionId())
                .then(validateNoDuplicateInventory(request.getProductOptionId()))
                .then(Mono.defer(() -> {
                    Inventory inventory = request.toEntity();
                    return inventoryRepository.save(inventory);
                }))
                .flatMap(savedInventory -> createInitialHistory(savedInventory)
                        .thenReturn(savedInventory))
                .map(InventoryResponse::from)
                .doOnSuccess(response -> log.info("재고 생성 완료: productOptionId={}, quantity={}",
                        request.getProductOptionId(), request.getQuantity()))
                .onErrorMap(this::mapToBusinessException);
    }

    // ========== 재고 조회 ==========

    /**
     * 재고 상세 조회
     */
    public Mono<InventoryResponse> getInventory(Long inventoryId) {
        return inventoryRepository.findById(inventoryId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVENTORY_NOT_FOUND)))
                .map(InventoryResponse::from);
    }

    /**
     * 상품 옵션별 재고 조회
     */
    public Mono<InventoryResponse> getInventoryByProductOption(Long productOptionId) {
        return inventoryRepository.findByProductOptionId(productOptionId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVENTORY_NOT_FOUND)))
                .map(InventoryResponse::from);
    }

    /**
     * 상품의 전체 옵션 재고 조회
     */
    public Flux<InventoryResponse> getInventoriesByProduct(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .map(InventoryResponse::from);
    }

    /**
     * 상품 옵션의 가용 재고 간단 조회
     */
    public Mono<InventoryStockResponse> getStockInfo(Long productOptionId) {
        return inventoryRepository.findByProductOptionId(productOptionId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVENTORY_NOT_FOUND)))
                .map(InventoryStockResponse::from);
    }

    /**
     * 주문 가능 여부 확인
     */
    public Mono<Boolean> canOrder(Long productOptionId, int quantity) {
        return inventoryRepository.findByProductOptionId(productOptionId)
                .map(inventory -> inventory.canOrder(quantity))
                .defaultIfEmpty(false);
    }

    /**
     * 품절 상품 목록 조회
     */
    public Flux<InventoryResponse> getSoldOutInventories() {
        return inventoryRepository.findSoldOutInventories()
                .map(InventoryResponse::from);
    }

    /**
     * 안전 재고 이하 상품 목록 조회
     */
    public Flux<InventoryResponse> getLowStockInventories() {
        return inventoryRepository.findLowStockInventories()
                .map(InventoryResponse::from);
    }

    // ========== 재고 증가 (입고) ==========

    /**
     * 재고 증가 (입고)
     */
    @Transactional
    public Mono<InventoryResponse> increaseStock(Long inventoryId, InventoryIncreaseRequest request) {
        return inventoryRepository.findByIdWithLock(inventoryId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVENTORY_NOT_FOUND)))
                .flatMap(inventory -> {
                    int beforeQuantity = inventory.getQuantity();
                    inventory.increase(request.getQuantity());

                    return inventoryRepository.save(inventory)
                            .flatMap(savedInventory -> {
                                InventoryHistory history = InventoryHistory.createIncreaseHistory(
                                        savedInventory, request.getQuantity(), beforeQuantity,
                                        request.getReason() != null ? request.getReason() : "입고"
                                );
                                return historyRepository.save(history).thenReturn(savedInventory);
                            });
                })
                .map(InventoryResponse::from)
                .doOnSuccess(response -> log.info("재고 증가: inventoryId={}, quantity={}",
                        inventoryId, request.getQuantity()))
                .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, RETRY_DELAY)
                        .filter(this::isRetryableException))
                .onErrorMap(this::mapToBusinessException);
    }

    /**
     * 상품 옵션 ID로 재고 증가
     */
    @Transactional
    public Mono<InventoryResponse> increaseStockByProductOption(Long productOptionId, InventoryIncreaseRequest request) {
        return inventoryRepository.findByProductOptionIdWithLock(productOptionId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVENTORY_NOT_FOUND)))
                .flatMap(inventory -> {
                    int beforeQuantity = inventory.getQuantity();
                    inventory.increase(request.getQuantity());

                    return inventoryRepository.save(inventory)
                            .flatMap(savedInventory -> {
                                InventoryHistory history = InventoryHistory.createIncreaseHistory(
                                        savedInventory, request.getQuantity(), beforeQuantity,
                                        request.getReason() != null ? request.getReason() : "입고"
                                );
                                return historyRepository.save(history).thenReturn(savedInventory);
                            });
                })
                .map(InventoryResponse::from)
                .doOnSuccess(response -> log.info("재고 증가: productOptionId={}, quantity={}",
                        productOptionId, request.getQuantity()))
                .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, RETRY_DELAY)
                        .filter(this::isRetryableException))
                .onErrorMap(this::mapToBusinessException);
    }

    // ========== 재고 감소 (출고) ==========

    /**
     * 재고 감소 (출고)
     */
    @Transactional
    public Mono<InventoryResponse> decreaseStock(Long inventoryId, InventoryDecreaseRequest request) {
        return inventoryRepository.findByIdWithLock(inventoryId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVENTORY_NOT_FOUND)))
                .flatMap(inventory -> {
                    int beforeQuantity = inventory.getQuantity();
                    inventory.decrease(request.getQuantity());

                    return inventoryRepository.save(inventory)
                            .flatMap(savedInventory -> {
                                InventoryHistory history = InventoryHistory.createDecreaseHistory(
                                        savedInventory, request.getQuantity(), beforeQuantity,
                                        request.getReason() != null ? request.getReason() : "출고",
                                        request.getReferenceId(), request.getReferenceType()
                                );
                                return historyRepository.save(history).thenReturn(savedInventory);
                            });
                })
                .map(InventoryResponse::from)
                .doOnSuccess(response -> log.info("재고 감소: inventoryId={}, quantity={}",
                        inventoryId, request.getQuantity()))
                .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, RETRY_DELAY)
                        .filter(this::isRetryableException))
                .onErrorMap(this::mapToBusinessException);
    }

    // ========== 재고 예약 (주문 시) ==========

    /**
     * 단일 상품 재고 예약
     */
    @Transactional
    public Mono<InventoryResponse> reserveStock(Long productOptionId, InventoryReserveRequest request) {
        return inventoryRepository.findByProductOptionIdWithLock(productOptionId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVENTORY_NOT_FOUND)))
                .flatMap(inventory -> {
                    int beforeAvailable = inventory.getAvailableQuantity();
                    inventory.reserve(request.getQuantity());

                    return inventoryRepository.save(inventory)
                            .flatMap(savedInventory -> {
                                InventoryHistory history = InventoryHistory.createReserveHistory(
                                        savedInventory, request.getQuantity(), beforeAvailable,
                                        request.getOrderId()
                                );
                                return historyRepository.save(history).thenReturn(savedInventory);
                            });
                })
                .map(InventoryResponse::from)
                .doOnSuccess(response -> log.info("재고 예약: productOptionId={}, quantity={}",
                        productOptionId, request.getQuantity()))
                .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, RETRY_DELAY)
                        .filter(this::isRetryableException))
                .onErrorMap(this::mapToBusinessException);
    }

    /**
     * 복수 상품 재고 일괄 예약 (주문 시)
     */
    @Transactional
    public Mono<List<InventoryResponse>> reserveStockBulk(BulkInventoryReserveRequest request) {
        return Flux.fromIterable(request.getItems())
                .flatMap(item -> {
                    InventoryReserveRequest reserveRequest = InventoryReserveRequest.builder()
                            .quantity(item.getQuantity())
                            .orderId(request.getOrderId())
                            .build();
                    return reserveStock(item.getProductOptionId(), reserveRequest);
                })
                .collectList()
                .doOnSuccess(responses -> log.info("일괄 재고 예약 완료: orderId={}, items={}",
                        request.getOrderId(), request.getItems().size()))
                .onErrorResume(e -> {
                    log.error("일괄 재고 예약 실패: orderId={}", request.getOrderId(), e);
                    // 예약 실패 시 이미 예약된 재고 해제 로직 추가 가능
                    return Mono.error(new BusinessException(ErrorCode.STOCK_RESERVATION_FAILED));
                });
    }

    // ========== 예약 해제 (주문 취소 시) ==========

    /**
     * 재고 예약 해제
     */
    @Transactional
    public Mono<InventoryResponse> releaseStock(Long productOptionId, int quantity, Long orderId, String reason) {
        return inventoryRepository.findByProductOptionIdWithLock(productOptionId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVENTORY_NOT_FOUND)))
                .flatMap(inventory -> {
                    int beforeAvailable = inventory.getAvailableQuantity();
                    inventory.release(quantity);

                    return inventoryRepository.save(inventory)
                            .flatMap(savedInventory -> {
                                InventoryHistory history = InventoryHistory.createReleaseHistory(
                                        savedInventory, quantity, beforeAvailable, orderId,
                                        reason != null ? reason : "주문 취소"
                                );
                                return historyRepository.save(history).thenReturn(savedInventory);
                            });
                })
                .map(InventoryResponse::from)
                .doOnSuccess(response -> log.info("재고 예약 해제: productOptionId={}, quantity={}",
                        productOptionId, quantity))
                .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, RETRY_DELAY)
                        .filter(this::isRetryableException))
                .onErrorMap(this::mapToBusinessException);
    }

    // ========== 예약 확정 (배송 출발 시) ==========

    /**
     * 예약 확정 - 실제 출고 처리
     */
    @Transactional
    public Mono<InventoryResponse> confirmReservation(Long productOptionId, int quantity, Long orderId) {
        return inventoryRepository.findByProductOptionIdWithLock(productOptionId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVENTORY_NOT_FOUND)))
                .flatMap(inventory -> {
                    int beforeQuantity = inventory.getQuantity();
                    inventory.confirmReservation(quantity);

                    return inventoryRepository.save(inventory)
                            .flatMap(savedInventory -> {
                                InventoryHistory history = InventoryHistory.createDecreaseHistory(
                                        savedInventory, quantity, beforeQuantity,
                                        "예약 확정 출고", orderId, "ORDER"
                                );
                                return historyRepository.save(history).thenReturn(savedInventory);
                            });
                })
                .map(InventoryResponse::from)
                .doOnSuccess(response -> log.info("예약 확정: productOptionId={}, quantity={}",
                        productOptionId, quantity))
                .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, RETRY_DELAY)
                        .filter(this::isRetryableException))
                .onErrorMap(this::mapToBusinessException);
    }

    // ========== 반품 입고 ==========

    /**
     * 반품 입고 처리
     */
    @Transactional
    public Mono<InventoryResponse> returnStock(Long productOptionId, int quantity, Long orderId) {
        return inventoryRepository.findByProductOptionIdWithLock(productOptionId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVENTORY_NOT_FOUND)))
                .flatMap(inventory -> {
                    int beforeQuantity = inventory.getQuantity();
                    inventory.increase(quantity);

                    return inventoryRepository.save(inventory)
                            .flatMap(savedInventory -> {
                                InventoryHistory history = InventoryHistory.createReturnHistory(
                                        savedInventory, quantity, beforeQuantity, orderId
                                );
                                return historyRepository.save(history).thenReturn(savedInventory);
                            });
                })
                .map(InventoryResponse::from)
                .doOnSuccess(response -> log.info("반품 입고: productOptionId={}, quantity={}",
                        productOptionId, quantity))
                .onErrorMap(this::mapToBusinessException);
    }

    // ========== 재고 조정 (관리자) ==========

    /**
     * 재고 직접 조정 (관리자용)
     */
    @Transactional
    public Mono<InventoryResponse> adjustStock(Long inventoryId, InventoryAdjustRequest request) {
        return inventoryRepository.findByIdWithLock(inventoryId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVENTORY_NOT_FOUND)))
                .flatMap(inventory -> {
                    int beforeQuantity = inventory.getQuantity();
                    inventory.adjust(request.getQuantity());

                    return inventoryRepository.save(inventory)
                            .flatMap(savedInventory -> {
                                InventoryHistory history = InventoryHistory.createAdjustHistory(
                                        savedInventory, beforeQuantity, request.getQuantity(),
                                        request.getReason()
                                );
                                return historyRepository.save(history).thenReturn(savedInventory);
                            });
                })
                .map(InventoryResponse::from)
                .doOnSuccess(response -> log.info("재고 조정: inventoryId={}, newQuantity={}",
                        inventoryId, request.getQuantity()))
                .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, RETRY_DELAY)
                        .filter(this::isRetryableException))
                .onErrorMap(this::mapToBusinessException);
    }

    // ========== 재고 이력 조회 ==========

    /**
     * 재고 이력 조회 (페이징)
     */
    public Mono<PageResponse<InventoryHistoryResponse>> getInventoryHistories(Long inventoryId, int page, int size) {
        int offset = page * size;

        Flux<InventoryHistoryResponse> histories = historyRepository
                .findByInventoryIdWithPaging(inventoryId, size, offset)
                .map(InventoryHistoryResponse::from);

        Mono<Long> totalCount = historyRepository.countByInventoryId(inventoryId);

        return histories.collectList()
                .zipWith(totalCount)
                .map(tuple -> PageResponse.of(tuple.getT1(), tuple.getT2(), page, size));
    }

    /**
     * 상품 옵션별 재고 이력 조회 (페이징)
     */
    public Mono<PageResponse<InventoryHistoryResponse>> getHistoriesByProductOption(Long productOptionId, int page, int size) {
        int offset = page * size;

        Flux<InventoryHistoryResponse> histories = historyRepository
                .findByProductOptionIdWithPaging(productOptionId, size, offset)
                .map(InventoryHistoryResponse::from);

        Mono<Long> totalCount = historyRepository.countByProductOptionId(productOptionId);

        return histories.collectList()
                .zipWith(totalCount)
                .map(tuple -> PageResponse.of(tuple.getT1(), tuple.getT2(), page, size));
    }

    // ========== Private Helper Methods ==========

    private Mono<Void> validateProductOptionExists(Long productOptionId) {
        return productOptionRepository.findById(productOptionId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.OPTION_NOT_FOUND)))
                .then();
    }

    private Mono<Void> validateNoDuplicateInventory(Long productOptionId) {
        return inventoryRepository.existsByProductOptionId(productOptionId)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new BusinessException(ErrorCode.DUPLICATE_INVENTORY));
                    }
                    return Mono.empty();
                });
    }

    private Mono<InventoryHistory> createInitialHistory(Inventory inventory) {
        if (inventory.getQuantity() > 0) {
            InventoryHistory history = InventoryHistory.createIncreaseHistory(
                    inventory, inventory.getQuantity(), 0, "초기 재고 등록"
            );
            return historyRepository.save(history);
        }
        return Mono.empty();
    }

    private boolean isRetryableException(Throwable e) {
        return e instanceof OptimisticLockingFailureException;
    }

    private Throwable mapToBusinessException(Throwable error) {
        if (error instanceof BusinessException) {
            return error;
        }
        if (error instanceof OptimisticLockingFailureException) {
            return new BusinessException(ErrorCode.INVENTORY_UPDATE_CONFLICT);
        }
        log.error("예상치 못한 오류 발생", error);
        return new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
