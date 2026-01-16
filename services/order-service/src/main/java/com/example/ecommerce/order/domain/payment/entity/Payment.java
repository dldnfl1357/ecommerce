package com.example.ecommerce.order.domain.payment.entity;

import com.example.ecommerce.common.core.entity.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table("payments")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Payment extends BaseEntity {

    @Id
    private Long id;

    @Column("order_id")
    private Long orderId;

    @Column("payment_key")
    private String paymentKey;

    @Column("method")
    private PaymentMethod method;

    @Column("status")
    private PaymentStatus status;

    @Column("amount")
    private BigDecimal amount;

    @Column("paid_amount")
    private BigDecimal paidAmount;

    @Column("refunded_amount")
    private BigDecimal refundedAmount;

    @Column("card_company")
    private String cardCompany;

    @Column("card_number")
    private String cardNumber;

    @Column("installment_months")
    private Integer installmentMonths;

    @Column("bank_name")
    private String bankName;

    @Column("account_number")
    private String accountNumber;

    @Column("virtual_account_holder")
    private String virtualAccountHolder;

    @Column("virtual_account_due_date")
    private LocalDateTime virtualAccountDueDate;

    @Column("paid_at")
    private LocalDateTime paidAt;

    @Column("cancelled_at")
    private LocalDateTime cancelledAt;

    @Column("refunded_at")
    private LocalDateTime refundedAt;

    @Column("failure_reason")
    private String failureReason;

    public Payment markAsPaid(String paymentKey, BigDecimal paidAmount) {
        this.paymentKey = paymentKey;
        this.status = PaymentStatus.PAID;
        this.paidAmount = paidAmount;
        this.paidAt = LocalDateTime.now();
        return this;
    }

    public Payment markAsCancelled(String reason) {
        this.status = PaymentStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.failureReason = reason;
        return this;
    }

    public Payment markAsRefunded(BigDecimal refundAmount) {
        this.refundedAmount = this.refundedAmount != null
                ? this.refundedAmount.add(refundAmount)
                : refundAmount;

        if (this.refundedAmount.compareTo(this.paidAmount) >= 0) {
            this.status = PaymentStatus.REFUNDED;
        } else {
            this.status = PaymentStatus.PARTIAL_REFUNDED;
        }
        this.refundedAt = LocalDateTime.now();
        return this;
    }

    public Payment markAsFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        return this;
    }

    public boolean canRefund() {
        return this.status == PaymentStatus.PAID ||
               this.status == PaymentStatus.PARTIAL_REFUNDED;
    }

    public BigDecimal getRefundableAmount() {
        if (!canRefund()) {
            return BigDecimal.ZERO;
        }
        BigDecimal refunded = this.refundedAmount != null ? this.refundedAmount : BigDecimal.ZERO;
        return this.paidAmount.subtract(refunded);
    }

    public static Payment create(Long orderId, PaymentMethod method, BigDecimal amount) {
        return Payment.builder()
                .orderId(orderId)
                .method(method)
                .status(PaymentStatus.PENDING)
                .amount(amount)
                .paidAmount(BigDecimal.ZERO)
                .refundedAmount(BigDecimal.ZERO)
                .build();
    }

    public static Payment createVirtualAccount(Long orderId, BigDecimal amount,
                                               String bankName, String accountNumber,
                                               String holder, LocalDateTime dueDate) {
        return Payment.builder()
                .orderId(orderId)
                .method(PaymentMethod.VIRTUAL_ACCOUNT)
                .status(PaymentStatus.WAITING_FOR_DEPOSIT)
                .amount(amount)
                .paidAmount(BigDecimal.ZERO)
                .refundedAmount(BigDecimal.ZERO)
                .bankName(bankName)
                .accountNumber(accountNumber)
                .virtualAccountHolder(holder)
                .virtualAccountDueDate(dueDate)
                .build();
    }
}
