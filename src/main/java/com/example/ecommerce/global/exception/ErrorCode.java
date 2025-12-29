package com.example.ecommerce.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C001", "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C002", "잘못된 입력값입니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C003", "잘못된 타입입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C004", "리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C005", "허용되지 않은 메서드입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "C006", "접근이 거부되었습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C007", "인증이 필요합니다."),

    // Member
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "회원을 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "M002", "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "M003", "비밀번호가 일치하지 않습니다."),
    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "M004", "적립금이 부족합니다."),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "상품을 찾을 수 없습니다."),
    PRODUCT_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "P002", "판매 중인 상품이 아닙니다."),
    OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "P003", "상품 옵션을 찾을 수 없습니다."),

    // Inventory
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "I001", "재고가 부족합니다."),
    STOCK_RESERVATION_FAILED(HttpStatus.CONFLICT, "I002", "재고 예약에 실패했습니다."),

    // Cart
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "CT001", "장바구니 상품을 찾을 수 없습니다."),

    // Order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "O001", "주문을 찾을 수 없습니다."),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "O002", "주문 상태가 올바르지 않습니다."),
    ORDER_CANNOT_BE_CANCELLED(HttpStatus.BAD_REQUEST, "O003", "취소할 수 없는 주문입니다."),
    ORDER_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "O004", "주문 상품을 찾을 수 없습니다."),

    // Payment
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PM001", "결제 정보를 찾을 수 없습니다."),
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "PM002", "결제에 실패했습니다."),
    REFUND_FAILED(HttpStatus.BAD_REQUEST, "PM003", "환불에 실패했습니다."),

    // Coupon
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "CP001", "쿠폰을 찾을 수 없습니다."),
    COUPON_ALREADY_USED(HttpStatus.BAD_REQUEST, "CP002", "이미 사용된 쿠폰입니다."),
    COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "CP003", "만료된 쿠폰입니다."),
    DUPLICATE_COUPON(HttpStatus.BAD_REQUEST, "CP004", "중복 적용할 수 없는 쿠폰입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
