package com.example.ecommerce.product.domain.inventory.controller;

import com.example.ecommerce.common.response.ApiResponse;
import com.example.ecommerce.product.domain.inventory.dto.request.InventoryAdjustRequest;
import com.example.ecommerce.product.domain.inventory.dto.request.InventoryReserveRequest;
import com.example.ecommerce.product.domain.inventory.dto.response.InventoryResponse;
import com.example.ecommerce.product.domain.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/option/{productOptionId}")
    public Mono<ApiResponse<InventoryResponse>> getInventory(@PathVariable Long productOptionId) {
        return inventoryService.getInventory(productOptionId)
                .map(ApiResponse::success);
    }

    @GetMapping("/low-stock")
    public Mono<ApiResponse<List<InventoryResponse>>> getLowStockInventory() {
        return inventoryService.getLowStockInventory()
                .collectList()
                .map(ApiResponse::success);
    }

    // Internal API - 주문 서비스에서 호출
    @PostMapping("/reserve")
    public Mono<ApiResponse<InventoryResponse>> reserveStock(
            @Valid @RequestBody InventoryReserveRequest request
    ) {
        log.info("재고 예약 요청: optionId={}, quantity={}", request.getProductOptionId(), request.getQuantity());
        return inventoryService.reserveStock(request)
                .map(response -> ApiResponse.success(response, "재고가 예약되었습니다."));
    }

    @PostMapping("/option/{productOptionId}/release")
    public Mono<ApiResponse<InventoryResponse>> releaseStock(
            @PathVariable Long productOptionId,
            @RequestParam Integer quantity,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String reason
    ) {
        log.info("재고 해제 요청: optionId={}, quantity={}", productOptionId, quantity);
        return inventoryService.releaseStock(productOptionId, quantity, orderId, reason)
                .map(response -> ApiResponse.success(response, "재고 예약이 해제되었습니다."));
    }

    @PostMapping("/option/{productOptionId}/increase")
    public Mono<ApiResponse<InventoryResponse>> increaseStock(
            @PathVariable Long productOptionId,
            @Valid @RequestBody InventoryAdjustRequest request
    ) {
        log.info("재고 증가 요청: optionId={}, quantity={}", productOptionId, request.getQuantity());
        return inventoryService.increaseStock(productOptionId, request)
                .map(response -> ApiResponse.success(response, "재고가 증가되었습니다."));
    }

    @PostMapping("/option/{productOptionId}/decrease")
    public Mono<ApiResponse<InventoryResponse>> decreaseStock(
            @PathVariable Long productOptionId,
            @Valid @RequestBody InventoryAdjustRequest request
    ) {
        log.info("재고 감소 요청: optionId={}, quantity={}", productOptionId, request.getQuantity());
        return inventoryService.decreaseStock(productOptionId, request)
                .map(response -> ApiResponse.success(response, "재고가 감소되었습니다."));
    }
}
