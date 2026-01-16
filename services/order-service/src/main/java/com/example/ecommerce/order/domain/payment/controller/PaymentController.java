package com.example.ecommerce.order.domain.payment.controller;

import com.example.ecommerce.common.core.response.ApiResponse;
import com.example.ecommerce.order.domain.payment.dto.request.PaymentConfirmRequest;
import com.example.ecommerce.order.domain.payment.dto.request.PaymentRequest;
import com.example.ecommerce.order.domain.payment.dto.request.RefundRequest;
import com.example.ecommerce.order.domain.payment.dto.response.PaymentResponse;
import com.example.ecommerce.order.domain.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<PaymentResponse>> initiatePayment(
            @Valid @RequestBody PaymentRequest request
    ) {
        log.info("결제 시작: orderId={}, method={}", request.getOrderId(), request.getMethod());
        return paymentService.initiatePayment(request)
                .map(response -> ApiResponse.success(response, "결제가 시작되었습니다."));
    }

    @PostMapping("/confirm")
    public Mono<ApiResponse<PaymentResponse>> confirmPayment(
            @Valid @RequestBody PaymentConfirmRequest request
    ) {
        log.info("결제 승인: orderId={}", request.getOrderId());
        return paymentService.confirmPayment(request)
                .map(response -> ApiResponse.success(response, "결제가 완료되었습니다."));
    }

    @GetMapping("/order/{orderId}")
    public Mono<ApiResponse<PaymentResponse>> getPaymentByOrderId(
            @PathVariable Long orderId
    ) {
        return paymentService.getPayment(orderId)
                .map(ApiResponse::success);
    }

    @GetMapping("/{paymentKey}")
    public Mono<ApiResponse<PaymentResponse>> getPaymentByPaymentKey(
            @PathVariable String paymentKey
    ) {
        return paymentService.getPaymentByPaymentKey(paymentKey)
                .map(ApiResponse::success);
    }

    @PostMapping("/order/{orderId}/cancel")
    public Mono<ApiResponse<PaymentResponse>> cancelPayment(
            @PathVariable Long orderId,
            @RequestParam String reason
    ) {
        log.info("결제 취소: orderId={}", orderId);
        return paymentService.cancelPayment(orderId, reason)
                .map(response -> ApiResponse.success(response, "결제가 취소되었습니다."));
    }

    @PostMapping("/order/{orderId}/refund")
    public Mono<ApiResponse<PaymentResponse>> refundPayment(
            @PathVariable Long orderId,
            @Valid @RequestBody RefundRequest request
    ) {
        log.info("환불 요청: orderId={}, amount={}", orderId, request.getAmount());
        return paymentService.refundPayment(orderId, request)
                .map(response -> ApiResponse.success(response, "환불이 처리되었습니다."));
    }

    // Webhook endpoint for PG callback
    @PostMapping("/webhook/virtual-account")
    public Mono<ApiResponse<PaymentResponse>> handleVirtualAccountDeposit(
            @RequestParam String paymentKey,
            @RequestParam BigDecimal amount
    ) {
        log.info("가상계좌 입금 webhook: paymentKey={}, amount={}", paymentKey, amount);
        return paymentService.handleVirtualAccountDeposit(paymentKey, amount)
                .map(response -> ApiResponse.success(response, "입금이 확인되었습니다."));
    }
}
