package com.example.ecommerce.order.domain.payment.repository;

import com.example.ecommerce.order.domain.payment.entity.Payment;
import com.example.ecommerce.order.domain.payment.entity.PaymentStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface PaymentRepository extends ReactiveCrudRepository<Payment, Long> {

    Mono<Payment> findByOrderId(Long orderId);

    Mono<Payment> findByPaymentKey(String paymentKey);

    Flux<Payment> findByStatus(PaymentStatus status);

    @Query("SELECT * FROM payments WHERE status = 'WAITING_FOR_DEPOSIT' AND virtual_account_due_date < :deadline")
    Flux<Payment> findExpiredVirtualAccountPayments(LocalDateTime deadline);
}
