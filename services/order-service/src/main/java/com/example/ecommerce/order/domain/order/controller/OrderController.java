package com.example.ecommerce.order.domain.order.controller;

import com.example.ecommerce.common.core.response.ApiResponse;
import com.example.ecommerce.order.domain.order.dto.request.OrderCancelRequest;
import com.example.ecommerce.order.domain.order.dto.request.OrderCreateRequest;
import com.example.ecommerce.order.domain.order.dto.response.OrderResponse;
import com.example.ecommerce.order.domain.order.entity.OrderStatus;
import com.example.ecommerce.order.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<OrderResponse>> createOrder(
            @RequestAttribute("memberId") Long memberId,
            @Valid @RequestBody OrderCreateRequest request
    ) {
        log.info("주문 생성 요청: memberId={}", memberId);
        return orderService.createOrder(memberId, request)
                .map(response -> ApiResponse.success(response, "주문이 생성되었습니다."));
    }

    @GetMapping("/{orderId}")
    public Mono<ApiResponse<OrderResponse>> getOrder(
            @RequestAttribute("memberId") Long memberId,
            @PathVariable Long orderId
    ) {
        return orderService.getOrder(orderId, memberId)
                .map(ApiResponse::success);
    }

    @GetMapping("/number/{orderNumber}")
    public Mono<ApiResponse<OrderResponse>> getOrderByNumber(
            @RequestAttribute("memberId") Long memberId,
            @PathVariable String orderNumber
    ) {
        return orderService.getOrderByOrderNumber(orderNumber, memberId)
                .map(ApiResponse::success);
    }

    @GetMapping("/my")
    public Mono<ApiResponse<List<OrderResponse>>> getMyOrders(
            @RequestAttribute("memberId") Long memberId
    ) {
        return orderService.getMyOrders(memberId)
                .collectList()
                .map(ApiResponse::success);
    }

    @GetMapping("/my/status/{status}")
    public Mono<ApiResponse<List<OrderResponse>>> getMyOrdersByStatus(
            @RequestAttribute("memberId") Long memberId,
            @PathVariable OrderStatus status
    ) {
        return orderService.getMyOrdersByStatus(memberId, status)
                .collectList()
                .map(ApiResponse::success);
    }

    @PostMapping("/{orderId}/cancel")
    public Mono<ApiResponse<OrderResponse>> cancelOrder(
            @RequestAttribute("memberId") Long memberId,
            @PathVariable Long orderId,
            @Valid @RequestBody OrderCancelRequest request
    ) {
        log.info("주문 취소 요청: orderId={}, memberId={}", orderId, memberId);
        return orderService.cancelOrder(orderId, memberId, request)
                .map(response -> ApiResponse.success(response, "주문이 취소되었습니다."));
    }

    // Internal API - for other services
    @PutMapping("/internal/{orderId}/status")
    public Mono<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status
    ) {
        log.info("주문 상태 변경 요청 (Internal): orderId={}, status={}", orderId, status);
        return orderService.updateOrderStatus(orderId, status)
                .map(response -> ApiResponse.success(response, "주문 상태가 변경되었습니다."));
    }
}
