package com.example.ecommerce.order.domain.delivery.controller;

import com.example.ecommerce.common.core.response.ApiResponse;
import com.example.ecommerce.order.domain.delivery.dto.request.DeliveryCreateRequest;
import com.example.ecommerce.order.domain.delivery.dto.request.DeliveryShipRequest;
import com.example.ecommerce.order.domain.delivery.dto.response.DeliveryResponse;
import com.example.ecommerce.order.domain.delivery.entity.DeliveryStatus;
import com.example.ecommerce.order.domain.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<DeliveryResponse>> createDelivery(
            @Valid @RequestBody DeliveryCreateRequest request
    ) {
        log.info("배송 생성 요청: orderId={}", request.getOrderId());
        return deliveryService.createDelivery(request)
                .map(response -> ApiResponse.success(response, "배송이 생성되었습니다."));
    }

    @GetMapping("/{deliveryId}")
    public Mono<ApiResponse<DeliveryResponse>> getDelivery(
            @PathVariable Long deliveryId
    ) {
        return deliveryService.getDelivery(deliveryId)
                .map(ApiResponse::success);
    }

    @GetMapping("/order/{orderId}")
    public Mono<ApiResponse<DeliveryResponse>> getDeliveryByOrderId(
            @PathVariable Long orderId
    ) {
        return deliveryService.getDeliveryByOrderId(orderId)
                .map(ApiResponse::success);
    }

    @GetMapping("/track/{trackingNumber}")
    public Mono<ApiResponse<DeliveryResponse>> trackDelivery(
            @PathVariable String trackingNumber
    ) {
        return deliveryService.trackDelivery(trackingNumber)
                .map(ApiResponse::success);
    }

    // Internal APIs for order fulfillment

    @PostMapping("/{deliveryId}/prepare")
    public Mono<ApiResponse<DeliveryResponse>> prepareDelivery(
            @PathVariable Long deliveryId
    ) {
        log.info("배송 준비 요청: deliveryId={}", deliveryId);
        return deliveryService.prepareDelivery(deliveryId)
                .map(response -> ApiResponse.success(response, "배송 준비가 시작되었습니다."));
    }

    @PostMapping("/{deliveryId}/ship")
    public Mono<ApiResponse<DeliveryResponse>> shipDelivery(
            @PathVariable Long deliveryId,
            @Valid @RequestBody DeliveryShipRequest request
    ) {
        log.info("배송 출발 요청: deliveryId={}", deliveryId);
        return deliveryService.shipDelivery(deliveryId, request)
                .map(response -> ApiResponse.success(response, "배송이 시작되었습니다."));
    }

    @PutMapping("/{deliveryId}/status")
    public Mono<ApiResponse<DeliveryResponse>> updateDeliveryStatus(
            @PathVariable Long deliveryId,
            @RequestParam DeliveryStatus status,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String description
    ) {
        log.info("배송 상태 변경: deliveryId={}, status={}", deliveryId, status);
        return deliveryService.updateDeliveryStatus(deliveryId, status, location, description)
                .map(response -> ApiResponse.success(response, "배송 상태가 변경되었습니다."));
    }

    @PostMapping("/{deliveryId}/return")
    public Mono<ApiResponse<DeliveryResponse>> requestReturn(
            @PathVariable Long deliveryId,
            @RequestParam String reason
    ) {
        log.info("반품 요청: deliveryId={}", deliveryId);
        return deliveryService.requestReturn(deliveryId, reason)
                .map(response -> ApiResponse.success(response, "반품 요청이 접수되었습니다."));
    }
}
