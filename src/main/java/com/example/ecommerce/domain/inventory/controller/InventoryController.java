package com.example.ecommerce.domain.inventory.controller;

import com.example.ecommerce.domain.inventory.dto.request.*;
import com.example.ecommerce.domain.inventory.dto.response.InventoryHistoryResponse;
import com.example.ecommerce.domain.inventory.dto.response.InventoryResponse;
import com.example.ecommerce.domain.inventory.dto.response.InventoryStockResponse;
import com.example.ecommerce.domain.inventory.service.InventoryService;
import com.example.ecommerce.global.common.ApiResponse;
import com.example.ecommerce.global.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // ========== 재고 생성 ==========

    /**
     * 재고 생성 (상품 옵션 등록 시)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<InventoryResponse>> createInventory(
            @Valid @RequestBody InventoryCreateRequest request
    ) {
        log.info("재고 생성 요청: productOptionId={}", request.getProductOptionId());
        return inventoryService.createInventory(request)
                .map(response -> ApiResponse.success(response, "재고가 생성되었습니다."));
    }

    // ========== 재고 조회 ==========

    /**
     * 재고 상세 조회
     */
    @GetMapping("/{inventoryId}")
    public Mono<ApiResponse<InventoryResponse>> getInventory(
            @PathVariable Long inventoryId
    ) {
        return inventoryService.getInventory(inventoryId)
                .map(ApiResponse::success);
    }

    /**
     * 상품 옵션별 재고 조회
     */
    @GetMapping("/product-options/{productOptionId}")
    public Mono<ApiResponse<InventoryResponse>> getInventoryByProductOption(
            @PathVariable Long productOptionId
    ) {
        return inventoryService.getInventoryByProductOption(productOptionId)
                .map(ApiResponse::success);
    }

    /**
     * 상품의 전체 옵션 재고 조회
     */
    @GetMapping("/products/{productId}")
    public Mono<ApiResponse<List<InventoryResponse>>> getInventoriesByProduct(
            @PathVariable Long productId
    ) {
        return inventoryService.getInventoriesByProduct(productId)
                .collectList()
                .map(ApiResponse::success);
    }

    /**
     * 상품 옵션의 가용 재고 간단 조회
     */
    @GetMapping("/product-options/{productOptionId}/stock")
    public Mono<ApiResponse<InventoryStockResponse>> getStockInfo(
            @PathVariable Long productOptionId
    ) {
        return inventoryService.getStockInfo(productOptionId)
                .map(ApiResponse::success);
    }

    /**
     * 주문 가능 여부 확인
     */
    @GetMapping("/product-options/{productOptionId}/check")
    public Mono<ApiResponse<Boolean>> checkOrderable(
            @PathVariable Long productOptionId,
            @RequestParam int quantity
    ) {
        return inventoryService.canOrder(productOptionId, quantity)
                .map(ApiResponse::success);
    }

    /**
     * 품절 상품 목록 조회
     */
    @GetMapping("/sold-out")
    public Mono<ApiResponse<List<InventoryResponse>>> getSoldOutInventories() {
        return inventoryService.getSoldOutInventories()
                .collectList()
                .map(ApiResponse::success);
    }

    /**
     * 안전 재고 이하 상품 목록 조회
     */
    @GetMapping("/low-stock")
    public Mono<ApiResponse<List<InventoryResponse>>> getLowStockInventories() {
        return inventoryService.getLowStockInventories()
                .collectList()
                .map(ApiResponse::success);
    }

    // ========== 재고 증가 (입고) ==========

    /**
     * 재고 증가 (입고)
     */
    @PostMapping("/{inventoryId}/increase")
    public Mono<ApiResponse<InventoryResponse>> increaseStock(
            @PathVariable Long inventoryId,
            @Valid @RequestBody InventoryIncreaseRequest request
    ) {
        log.info("재고 증가 요청: inventoryId={}, quantity={}", inventoryId, request.getQuantity());
        return inventoryService.increaseStock(inventoryId, request)
                .map(response -> ApiResponse.success(response, "재고가 증가되었습니다."));
    }

    /**
     * 상품 옵션 ID로 재고 증가
     */
    @PostMapping("/product-options/{productOptionId}/increase")
    public Mono<ApiResponse<InventoryResponse>> increaseStockByProductOption(
            @PathVariable Long productOptionId,
            @Valid @RequestBody InventoryIncreaseRequest request
    ) {
        log.info("재고 증가 요청: productOptionId={}, quantity={}", productOptionId, request.getQuantity());
        return inventoryService.increaseStockByProductOption(productOptionId, request)
                .map(response -> ApiResponse.success(response, "재고가 증가되었습니다."));
    }

    // ========== 재고 감소 (출고) ==========

    /**
     * 재고 감소 (출고)
     */
    @PostMapping("/{inventoryId}/decrease")
    public Mono<ApiResponse<InventoryResponse>> decreaseStock(
            @PathVariable Long inventoryId,
            @Valid @RequestBody InventoryDecreaseRequest request
    ) {
        log.info("재고 감소 요청: inventoryId={}, quantity={}", inventoryId, request.getQuantity());
        return inventoryService.decreaseStock(inventoryId, request)
                .map(response -> ApiResponse.success(response, "재고가 감소되었습니다."));
    }

    // ========== 재고 예약 (주문 시) ==========

    /**
     * 단일 상품 재고 예약
     */
    @PostMapping("/product-options/{productOptionId}/reserve")
    public Mono<ApiResponse<InventoryResponse>> reserveStock(
            @PathVariable Long productOptionId,
            @Valid @RequestBody InventoryReserveRequest request
    ) {
        log.info("재고 예약 요청: productOptionId={}, quantity={}", productOptionId, request.getQuantity());
        return inventoryService.reserveStock(productOptionId, request)
                .map(response -> ApiResponse.success(response, "재고가 예약되었습니다."));
    }

    /**
     * 복수 상품 재고 일괄 예약 (주문 시)
     */
    @PostMapping("/reserve-bulk")
    public Mono<ApiResponse<List<InventoryResponse>>> reserveStockBulk(
            @Valid @RequestBody BulkInventoryReserveRequest request
    ) {
        log.info("일괄 재고 예약 요청: orderId={}, items={}", request.getOrderId(), request.getItems().size());
        return inventoryService.reserveStockBulk(request)
                .map(response -> ApiResponse.success(response, "일괄 재고 예약이 완료되었습니다."));
    }

    // ========== 예약 해제 (주문 취소 시) ==========

    /**
     * 재고 예약 해제
     */
    @PostMapping("/product-options/{productOptionId}/release")
    public Mono<ApiResponse<InventoryResponse>> releaseStock(
            @PathVariable Long productOptionId,
            @RequestParam int quantity,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String reason
    ) {
        log.info("재고 예약 해제 요청: productOptionId={}, quantity={}", productOptionId, quantity);
        return inventoryService.releaseStock(productOptionId, quantity, orderId, reason)
                .map(response -> ApiResponse.success(response, "재고 예약이 해제되었습니다."));
    }

    // ========== 예약 확정 (배송 출발 시) ==========

    /**
     * 예약 확정 - 실제 출고 처리
     */
    @PostMapping("/product-options/{productOptionId}/confirm")
    public Mono<ApiResponse<InventoryResponse>> confirmReservation(
            @PathVariable Long productOptionId,
            @RequestParam int quantity,
            @RequestParam Long orderId
    ) {
        log.info("예약 확정 요청: productOptionId={}, quantity={}", productOptionId, quantity);
        return inventoryService.confirmReservation(productOptionId, quantity, orderId)
                .map(response -> ApiResponse.success(response, "예약이 확정되었습니다."));
    }

    // ========== 반품 입고 ==========

    /**
     * 반품 입고 처리
     */
    @PostMapping("/product-options/{productOptionId}/return")
    public Mono<ApiResponse<InventoryResponse>> returnStock(
            @PathVariable Long productOptionId,
            @RequestParam int quantity,
            @RequestParam Long orderId
    ) {
        log.info("반품 입고 요청: productOptionId={}, quantity={}", productOptionId, quantity);
        return inventoryService.returnStock(productOptionId, quantity, orderId)
                .map(response -> ApiResponse.success(response, "반품 입고가 완료되었습니다."));
    }

    // ========== 재고 조정 (관리자) ==========

    /**
     * 재고 직접 조정 (관리자용)
     */
    @PutMapping("/{inventoryId}/adjust")
    public Mono<ApiResponse<InventoryResponse>> adjustStock(
            @PathVariable Long inventoryId,
            @Valid @RequestBody InventoryAdjustRequest request
    ) {
        log.info("재고 조정 요청: inventoryId={}, quantity={}", inventoryId, request.getQuantity());
        return inventoryService.adjustStock(inventoryId, request)
                .map(response -> ApiResponse.success(response, "재고가 조정되었습니다."));
    }

    // ========== 재고 이력 조회 ==========

    /**
     * 재고 이력 조회 (페이징)
     */
    @GetMapping("/{inventoryId}/histories")
    public Mono<ApiResponse<PageResponse<InventoryHistoryResponse>>> getInventoryHistories(
            @PathVariable Long inventoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return inventoryService.getInventoryHistories(inventoryId, page, size)
                .map(ApiResponse::success);
    }

    /**
     * 상품 옵션별 재고 이력 조회 (페이징)
     */
    @GetMapping("/product-options/{productOptionId}/histories")
    public Mono<ApiResponse<PageResponse<InventoryHistoryResponse>>> getHistoriesByProductOption(
            @PathVariable Long productOptionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return inventoryService.getHistoriesByProductOption(productOptionId, page, size)
                .map(ApiResponse::success);
    }
}
