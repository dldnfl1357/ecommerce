package com.example.ecommerce.order.domain.order.entity;

public enum OrderItemStatus {
    ORDERED("주문됨"),
    PREPARING("준비중"),
    SHIPPED("배송중"),
    DELIVERED("배송완료"),
    CANCELLED("취소됨"),
    RETURN_REQUESTED("반품 요청"),
    RETURNED("반품 완료"),
    EXCHANGE_REQUESTED("교환 요청"),
    EXCHANGED("교환 완료");

    private final String description;

    OrderItemStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
