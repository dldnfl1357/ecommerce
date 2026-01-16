package com.example.ecommerce.member.domain.member.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum MemberGrade {
    BRONZE(1, 0, 0.01, "브론즈"),
    SILVER(2, 100_000, 0.02, "실버"),
    GOLD(3, 500_000, 0.03, "골드"),
    VIP(4, 1_000_000, 0.05, "VIP");

    private final int level;
    private final int threshold;  // 최근 6개월 구매 금액 기준
    private final double pointRate;  // 적립률
    private final String displayName;

    /**
     * 구매 금액 기준으로 등급 계산 (함수형 스타일)
     */
    public static MemberGrade calculateGrade(int totalPurchaseAmount) {
        return Arrays.stream(values())
            .filter(grade -> totalPurchaseAmount >= grade.threshold)
            .reduce((first, second) -> second)  // 가장 높은 등급 선택
            .orElse(BRONZE);
    }

    /**
     * 다음 등급까지 필요한 금액 계산
     */
    public int getAmountToNextGrade(int currentAmount) {
        return Arrays.stream(values())
            .filter(grade -> grade.level == this.level + 1)
            .findFirst()
            .map(nextGrade -> Math.max(0, nextGrade.threshold - currentAmount))
            .orElse(0);  // 이미 최고 등급
    }

    /**
     * 적립금 계산
     */
    public int calculatePoint(int purchaseAmount) {
        return (int) (purchaseAmount * pointRate);
    }
}
