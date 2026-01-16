package com.example.ecommerce.order.domain.payment.service;

import com.example.ecommerce.common.core.exception.BusinessException;
import com.example.ecommerce.common.core.exception.ErrorCode;
import com.example.ecommerce.order.domain.order.entity.OrderStatus;
import com.example.ecommerce.order.domain.order.repository.OrderRepository;
import com.example.ecommerce.order.domain.payment.dto.request.PaymentConfirmRequest;
import com.example.ecommerce.order.domain.payment.dto.request.PaymentRequest;
import com.example.ecommerce.order.domain.payment.dto.request.RefundRequest;
import com.example.ecommerce.order.domain.payment.dto.response.PaymentResponse;
import com.example.ecommerce.order.domain.payment.entity.Payment;
import com.example.ecommerce.order.domain.payment.entity.PaymentMethod;
import com.example.ecommerce.order.domain.payment.entity.PaymentStatus;
import com.example.ecommerce.order.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public Mono<PaymentResponse> initiatePayment(PaymentRequest request) {
        return orderRepository.findById(request.getOrderId())
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.ORDER_NOT_FOUND)))
                .flatMap(order -> {
                    if (order.getStatus() != OrderStatus.PENDING) {
                        return Mono.error(new BusinessException(ErrorCode.INVALID_ORDER_STATUS));
                    }
                    return paymentRepository.findByOrderId(order.getId())
                            .flatMap(existingPayment -> {
                                if (existingPayment.getStatus() == PaymentStatus.PAID) {
                                    return Mono.error(new BusinessException(ErrorCode.PAYMENT_ALREADY_COMPLETED));
                                }
                                return Mono.just(existingPayment);
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                Payment payment;
                                if (request.getMethod() == PaymentMethod.VIRTUAL_ACCOUNT) {
                                    payment = Payment.createVirtualAccount(
                                            order.getId(),
                                            request.getAmount(),
                                            getBankName(request.getBankCode()),
                                            generateVirtualAccountNumber(),
                                            "쿠팡",
                                            LocalDateTime.now().plusDays(1)
                                    );
                                } else {
                                    payment = Payment.create(
                                            order.getId(),
                                            request.getMethod(),
                                            request.getAmount()
                                    );
                                }
                                return paymentRepository.save(payment);
                            }));
                })
                .map(PaymentResponse::from)
                .doOnSuccess(response -> log.info("결제 초기화: orderId={}, method={}",
                        request.getOrderId(), request.getMethod()));
    }

    @Transactional
    public Mono<PaymentResponse> confirmPayment(PaymentConfirmRequest request) {
        return paymentRepository.findByOrderId(request.getOrderId())
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PAYMENT_NOT_FOUND)))
                .flatMap(payment -> {
                    if (payment.getStatus() == PaymentStatus.PAID) {
                        return Mono.error(new BusinessException(ErrorCode.PAYMENT_ALREADY_COMPLETED));
                    }
                    if (payment.getAmount().compareTo(request.getAmount()) != 0) {
                        return Mono.error(new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH));
                    }

                    Payment updated = payment.markAsPaid(request.getPaymentKey(), request.getAmount());
                    return paymentRepository.save(updated);
                })
                .flatMap(payment -> orderRepository.findById(payment.getOrderId())
                        .flatMap(order -> {
                            order.markAsPaid();
                            return orderRepository.save(order);
                        })
                        .thenReturn(payment))
                .map(PaymentResponse::from)
                .doOnSuccess(response -> log.info("결제 완료: orderId={}, paymentKey={}",
                        request.getOrderId(), request.getPaymentKey()));
    }

    @Transactional
    public Mono<PaymentResponse> cancelPayment(Long orderId, String reason) {
        return paymentRepository.findByOrderId(orderId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PAYMENT_NOT_FOUND)))
                .flatMap(payment -> {
                    if (payment.getStatus() != PaymentStatus.PENDING &&
                        payment.getStatus() != PaymentStatus.WAITING_FOR_DEPOSIT) {
                        return Mono.error(new BusinessException(ErrorCode.PAYMENT_CANNOT_CANCEL));
                    }

                    Payment updated = payment.markAsCancelled(reason);
                    return paymentRepository.save(updated);
                })
                .map(PaymentResponse::from)
                .doOnSuccess(response -> log.info("결제 취소: orderId={}", orderId));
    }

    @Transactional
    public Mono<PaymentResponse> refundPayment(Long orderId, RefundRequest request) {
        return paymentRepository.findByOrderId(orderId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PAYMENT_NOT_FOUND)))
                .flatMap(payment -> {
                    if (!payment.canRefund()) {
                        return Mono.error(new BusinessException(ErrorCode.PAYMENT_CANNOT_REFUND));
                    }
                    if (request.getAmount().compareTo(payment.getRefundableAmount()) > 0) {
                        return Mono.error(new BusinessException(ErrorCode.REFUND_AMOUNT_EXCEEDED));
                    }

                    Payment updated = payment.markAsRefunded(request.getAmount());
                    return paymentRepository.save(updated);
                })
                .map(PaymentResponse::from)
                .doOnSuccess(response -> log.info("환불 처리: orderId={}, amount={}",
                        orderId, request.getAmount()));
    }

    public Mono<PaymentResponse> getPayment(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PAYMENT_NOT_FOUND)))
                .map(PaymentResponse::from);
    }

    public Mono<PaymentResponse> getPaymentByPaymentKey(String paymentKey) {
        return paymentRepository.findByPaymentKey(paymentKey)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PAYMENT_NOT_FOUND)))
                .map(PaymentResponse::from);
    }

    // Webhook callback from PG
    @Transactional
    public Mono<PaymentResponse> handleVirtualAccountDeposit(String paymentKey, BigDecimal amount) {
        return paymentRepository.findByPaymentKey(paymentKey)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PAYMENT_NOT_FOUND)))
                .flatMap(payment -> {
                    if (payment.getStatus() != PaymentStatus.WAITING_FOR_DEPOSIT) {
                        return Mono.error(new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS));
                    }
                    if (payment.getAmount().compareTo(amount) != 0) {
                        return Mono.error(new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH));
                    }

                    Payment updated = payment.markAsPaid(paymentKey, amount);
                    return paymentRepository.save(updated);
                })
                .flatMap(payment -> orderRepository.findById(payment.getOrderId())
                        .flatMap(order -> {
                            order.markAsPaid();
                            return orderRepository.save(order);
                        })
                        .thenReturn(payment))
                .map(PaymentResponse::from)
                .doOnSuccess(response -> log.info("가상계좌 입금 확인: paymentKey={}, amount={}",
                        paymentKey, amount));
    }

    private String getBankName(String bankCode) {
        return switch (bankCode) {
            case "004" -> "KB국민은행";
            case "011" -> "NH농협은행";
            case "020" -> "우리은행";
            case "081" -> "하나은행";
            case "088" -> "신한은행";
            default -> "기타은행";
        };
    }

    private String generateVirtualAccountNumber() {
        return "9" + String.format("%012d", System.currentTimeMillis() % 1000000000000L);
    }
}
