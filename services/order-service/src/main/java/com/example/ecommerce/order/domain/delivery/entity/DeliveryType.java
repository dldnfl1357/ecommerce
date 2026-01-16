package com.example.ecommerce.order.domain.delivery.entity;

public enum DeliveryType {
    ROCKET("로켓배송"),
    ROCKET_FRESH("로켓프레시"),
    REGULAR("일반배송"),
    SELLER("판매자배송");

    private final String description;

    DeliveryType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
