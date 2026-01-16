package com.example.ecommerce.order.domain.payment.entity;

public enum PaymentStatus {
    PENDING("결제 대기"),
    WAITING_FOR_DEPOSIT("입금 대기"),
    PAID("결제 완료"),
    CANCELLED("결제 취소"),
    FAILED("결제 실패"),
    PARTIAL_REFUNDED("부분 환불"),
    REFUNDED("전액 환불");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
