package com.example.ecommerce.domain.seller.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SellerStatus {

    PENDING("승인대기", "판매자 등록 심사 중"),
    ACTIVE("활성", "정상 영업 중"),
    SUSPENDED("정지", "영업 정지 상태"),
    WITHDRAWN("탈퇴", "판매자 탈퇴");

    private final String displayName;
    private final String description;

    public boolean canSell() {
        return this == ACTIVE;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }
}
