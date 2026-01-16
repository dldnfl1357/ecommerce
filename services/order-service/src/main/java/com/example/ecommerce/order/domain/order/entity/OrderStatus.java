package com.example.ecommerce.order.domain.order.entity;

public enum OrderStatus {
    PENDING("주문 대기"),
    PAID("결제 완료"),
    PREPARING("상품 준비중"),
    SHIPPED("배송중"),
    DELIVERED("배송 완료"),
    COMPLETED("구매 확정"),
    CANCELLED("주문 취소"),
    REFUND_REQUESTED("환불 요청"),
    REFUNDED("환불 완료");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
