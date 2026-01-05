package com.example.ecommerce.domain.product.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductStatus {

    ON_SALE("판매중", "정상 판매 중인 상품"),
    STOP_SALE("판매중지", "판매자에 의해 판매 중지"),
    SOLD_OUT("품절", "재고 소진으로 품절"),
    DELETED("삭제", "삭제된 상품");

    private final String displayName;
    private final String description;

    /**
     * 구매 가능 여부
     */
    public boolean canPurchase() {
        return this == ON_SALE;
    }

    /**
     * 판매 가능한 상태인지 확인
     */
    public boolean isAvailable() {
        return this == ON_SALE;
    }

    /**
     * 삭제된 상태인지 확인
     */
    public boolean isDeleted() {
        return this == DELETED;
    }

    /**
     * 품절 상태인지 확인
     */
    public boolean isSoldOut() {
        return this == SOLD_OUT;
    }
}
