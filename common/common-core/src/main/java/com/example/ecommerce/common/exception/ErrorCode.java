package com.example.ecommerce.common.exception;

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
    DUPLICATE_PHONE(HttpStatus.CONFLICT, "M003", "이미 사용 중인 전화번호입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "M004", "비밀번호가 일치하지 않습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "M005", "이메일 또는 비밀번호가 일치하지 않습니다."),
    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "M006", "적립금이 부족합니다."),
    INSUFFICIENT_POINT_MINIMUM(HttpStatus.BAD_REQUEST, "M007", "적립금은 최소 1,000원 이상 사용 가능합니다."),
    DORMANT_MEMBER(HttpStatus.FORBIDDEN, "M008", "휴면 계정입니다. 고객센터에 문의해주세요."),
    WITHDRAWN_MEMBER(HttpStatus.FORBIDDEN, "M009", "탈퇴한 회원입니다."),
    SUSPENDED_MEMBER(HttpStatus.FORBIDDEN, "M010", "이용 정지된 계정입니다."),

    // Auth
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A001", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "만료된 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "A003", "리프레시 토큰을 찾을 수 없습니다."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "A004", "인증에 실패했습니다."),

    // Address
    ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "AD001", "배송지를 찾을 수 없습니다."),
    DEFAULT_ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "AD002", "기본 배송지가 없습니다."),
    MAX_ADDRESS_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "AD003", "배송지는 최대 10개까지 등록 가능합니다."),
    CANNOT_DELETE_DEFAULT_ADDRESS(HttpStatus.BAD_REQUEST, "AD004", "다른 배송지를 기본으로 설정한 후 삭제해주세요."),
    UNAUTHORIZED_ADDRESS_ACCESS(HttpStatus.FORBIDDEN, "AD005", "배송지에 접근 권한이 없습니다."),

    // Seller
    SELLER_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "판매자를 찾을 수 없습니다."),
    DUPLICATE_BUSINESS_NUMBER(HttpStatus.CONFLICT, "S002", "이미 등록된 사업자등록번호입니다."),
    ALREADY_SELLER(HttpStatus.CONFLICT, "S003", "이미 판매자로 등록되어 있습니다."),
    SELLER_NOT_ACTIVE(HttpStatus.FORBIDDEN, "S004", "활성화된 판매자가 아닙니다."),
    SELLER_SUSPENDED(HttpStatus.FORBIDDEN, "S005", "정지된 판매자입니다."),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "상품을 찾을 수 없습니다."),
    PRODUCT_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "P002", "판매 중인 상품이 아닙니다."),
    OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "P003", "상품 옵션을 찾을 수 없습니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "P004", "카테고리를 찾을 수 없습니다."),
    INVALID_PRODUCT_PRICE(HttpStatus.BAD_REQUEST, "P005", "유효하지 않은 상품 가격입니다."),
    INVALID_DISCOUNT_RATE(HttpStatus.BAD_REQUEST, "P006", "할인율은 0~100 사이여야 합니다."),
    PRODUCT_IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "P007", "최소 1개의 상품 이미지가 필요합니다."),
    PRODUCT_OPTION_REQUIRED(HttpStatus.BAD_REQUEST, "P008", "최소 1개의 상품 옵션이 필요합니다."),
    PRODUCT_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "P009", "이미 삭제된 상품입니다."),
    CANNOT_DELETE_PRODUCT_WITH_ORDERS(HttpStatus.CONFLICT, "P010", "진행 중인 주문이 있어 삭제할 수 없습니다."),

    // Inventory
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "I001", "재고가 부족합니다."),
    STOCK_RESERVATION_FAILED(HttpStatus.CONFLICT, "I002", "재고 예약에 실패했습니다."),
    INVENTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "I003", "재고 정보를 찾을 수 없습니다."),
    DUPLICATE_INVENTORY(HttpStatus.CONFLICT, "I004", "이미 재고가 등록된 상품 옵션입니다."),
    INVALID_INVENTORY_QUANTITY(HttpStatus.BAD_REQUEST, "I005", "유효하지 않은 재고 수량입니다."),
    INVALID_INVENTORY_OPERATION(HttpStatus.BAD_REQUEST, "I006", "유효하지 않은 재고 작업입니다."),
    INVENTORY_UPDATE_CONFLICT(HttpStatus.CONFLICT, "I007", "재고 업데이트 충돌이 발생했습니다. 다시 시도해주세요."),

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
