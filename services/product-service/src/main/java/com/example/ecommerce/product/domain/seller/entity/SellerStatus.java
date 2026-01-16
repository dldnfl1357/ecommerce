package com.example.ecommerce.product.domain.seller.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SellerStatus {
    PENDING("심사중", "판매자 등록 심사 중"),
    ACTIVE("활성", "정상 활동 중인 판매자"),
    SUSPENDED("정지", "활동 정지된 판매자"),
    REJECTED("거절", "등록 거절된 판매자");

    private final String displayName;
    private final String description;
}
