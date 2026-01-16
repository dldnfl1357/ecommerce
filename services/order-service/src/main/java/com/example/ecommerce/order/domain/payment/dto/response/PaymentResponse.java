package com.example.ecommerce.order.domain.payment.dto.response;

import com.example.ecommerce.order.domain.payment.entity.Payment;
import com.example.ecommerce.order.domain.payment.entity.PaymentMethod;
import com.example.ecommerce.order.domain.payment.entity.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private Long orderId;
    private String paymentKey;
    private PaymentMethod method;
    private String methodDescription;
    private PaymentStatus status;
    private String statusDescription;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private BigDecimal refundedAmount;
    private BigDecimal refundableAmount;
    private String cardCompany;
    private String cardNumber;
    private Integer installmentMonths;
    private String bankName;
    private String accountNumber;
    private String virtualAccountHolder;
    private LocalDateTime virtualAccountDueDate;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime refundedAt;
    private String failureReason;

    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .paymentKey(payment.getPaymentKey())
                .method(payment.getMethod())
                .methodDescription(payment.getMethod().getDescription())
                .status(payment.getStatus())
                .statusDescription(payment.getStatus().getDescription())
                .amount(payment.getAmount())
                .paidAmount(payment.getPaidAmount())
                .refundedAmount(payment.getRefundedAmount())
                .refundableAmount(payment.getRefundableAmount())
                .cardCompany(payment.getCardCompany())
                .cardNumber(maskCardNumber(payment.getCardNumber()))
                .installmentMonths(payment.getInstallmentMonths())
                .bankName(payment.getBankName())
                .accountNumber(payment.getAccountNumber())
                .virtualAccountHolder(payment.getVirtualAccountHolder())
                .virtualAccountDueDate(payment.getVirtualAccountDueDate())
                .paidAt(payment.getPaidAt())
                .cancelledAt(payment.getCancelledAt())
                .refundedAt(payment.getRefundedAt())
                .failureReason(payment.getFailureReason())
                .build();
    }

    private static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return cardNumber;
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}
