package com.example.ecommerce.order.domain.payment.entity;

public enum PaymentMethod {
    CARD("신용/체크카드"),
    VIRTUAL_ACCOUNT("가상계좌"),
    BANK_TRANSFER("계좌이체"),
    PHONE("휴대폰결제"),
    KAKAO_PAY("카카오페이"),
    NAVER_PAY("네이버페이"),
    TOSS("토스"),
    POINT("포인트");

    private final String description;

    PaymentMethod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
