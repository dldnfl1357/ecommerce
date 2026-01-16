package com.example.ecommerce.order.domain.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CartItemAddRequest {

    @NotNull(message = "상품 ID는 필수입니다")
    private Long productId;

    @NotNull(message = "상품 옵션 ID는 필수입니다")
    private Long productOptionId;

    @NotNull(message = "수량은 필수입니다")
    @Min(value = 1, message = "수량은 1 이상이어야 합니다")
    private Integer quantity;
}
