package com.example.ecommerce.domain.inventory.entity;

/**
 * 재고 변경 유형
 */
public enum InventoryChangeType {

    INCREASE("입고"),
    DECREASE("출고"),
    RESERVE("예약"),
    RELEASE("예약 해제"),
    ADJUST("재고 조정"),
    RETURN("반품 입고");

    private final String description;

    InventoryChangeType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
