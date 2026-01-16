package com.example.ecommerce.product.domain.product.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductStatus {
    DRAFT("임시저장", "등록 중인 상품"),
    ACTIVE("판매중", "정상 판매 중인 상품"),
    SOLD_OUT("품절", "재고 소진된 상품"),
    SUSPENDED("판매중지", "판매 중지된 상품"),
    DELETED("삭제", "삭제된 상품");

    private final String displayName;
    private final String description;

    public boolean canSell() {
        return this == ACTIVE;
    }
}
