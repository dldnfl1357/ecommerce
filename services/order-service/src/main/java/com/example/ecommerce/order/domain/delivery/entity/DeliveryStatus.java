package com.example.ecommerce.order.domain.delivery.entity;

public enum DeliveryStatus {
    PENDING("배송 대기"),
    PREPARING("상품 준비중"),
    SHIPPED("배송중"),
    OUT_FOR_DELIVERY("배송 출발"),
    DELIVERED("배송 완료"),
    RETURN_REQUESTED("반품 요청"),
    RETURNING("반품 배송중"),
    RETURNED("반품 완료");

    private final String description;

    DeliveryStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
