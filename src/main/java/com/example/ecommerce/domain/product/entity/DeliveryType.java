package com.example.ecommerce.domain.product.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DeliveryType {

    ROCKET("로켓배송", 0, 19_800, "내일 도착"),
    ROCKET_FRESH("로켓프레시", 0, 15_000, "신선식품 새벽배송"),
    DAWN("새벽배송", 0, 30_000, "새벽 도착"),
    NORMAL("일반배송", 3_000, 30_000, "2-3일 소요");

    private final String displayName;
    private final int baseFee;
    private final int freeThreshold;
    private final String description;

    /**
     * 배송비 계산
     *
     * @param orderAmount 주문 금액
     * @param isRocketWow 로켓와우 회원 여부
     * @return 배송비
     */
    public int calculateDeliveryFee(int orderAmount, boolean isRocketWow) {
        // 로켓와우 회원은 로켓배송/로켓프레시 무료
        if (isRocketWow && (this == ROCKET || this == ROCKET_FRESH)) {
            return 0;
        }
        // 무료배송 기준금액 이상이면 무료
        return orderAmount >= freeThreshold ? 0 : baseFee;
    }

    /**
     * 무료배송 여부 확인
     */
    public boolean isFreeDelivery(int orderAmount, boolean isRocketWow) {
        return calculateDeliveryFee(orderAmount, isRocketWow) == 0;
    }

    /**
     * 로켓배송 계열인지 확인
     */
    public boolean isRocketDelivery() {
        return this == ROCKET || this == ROCKET_FRESH;
    }
}
