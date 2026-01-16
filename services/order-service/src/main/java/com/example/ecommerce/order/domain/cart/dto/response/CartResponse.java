package com.example.ecommerce.order.domain.cart.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CartResponse {

    private Long id;
    private Long memberId;
    private List<CartItemResponse> items;
    private Integer totalItemCount;
    private Integer selectedItemCount;
    private BigDecimal totalPrice;
    private BigDecimal selectedPrice;
    private BigDecimal deliveryFee;
    private BigDecimal finalPrice;

    private static final BigDecimal FREE_DELIVERY_THRESHOLD = BigDecimal.valueOf(30000);
    private static final BigDecimal DELIVERY_FEE = BigDecimal.valueOf(3000);

    public static CartResponse of(Long cartId, Long memberId, List<CartItemResponse> items) {
        int totalItemCount = items.size();
        int selectedItemCount = (int) items.stream().filter(CartItemResponse::getIsSelected).count();

        BigDecimal totalPrice = items.stream()
                .map(CartItemResponse::getFinalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal selectedPrice = items.stream()
                .filter(CartItemResponse::getIsSelected)
                .map(CartItemResponse::getFinalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal deliveryFee = selectedPrice.compareTo(FREE_DELIVERY_THRESHOLD) >= 0
                ? BigDecimal.ZERO : DELIVERY_FEE;

        BigDecimal finalPrice = selectedPrice.add(deliveryFee);

        return CartResponse.builder()
                .id(cartId)
                .memberId(memberId)
                .items(items)
                .totalItemCount(totalItemCount)
                .selectedItemCount(selectedItemCount)
                .totalPrice(totalPrice)
                .selectedPrice(selectedPrice)
                .deliveryFee(deliveryFee)
                .finalPrice(finalPrice)
                .build();
    }
}
