package com.example.ecommerce.order.domain.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class OrderCreateRequest {

    @NotNull(message = "배송지 ID는 필수입니다")
    private Long addressId;

    @NotEmpty(message = "주문 상품은 최소 1개 이상이어야 합니다")
    @Valid
    private List<OrderItemRequest> items;

    private Long couponId;

    private Integer pointToUse;

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class OrderItemRequest {

        @NotNull(message = "상품 ID는 필수입니다")
        private Long productId;

        @NotNull(message = "상품 옵션 ID는 필수입니다")
        private Long productOptionId;

        @NotNull(message = "수량은 필수입니다")
        private Integer quantity;
    }
}
