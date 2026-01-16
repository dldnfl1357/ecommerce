package com.example.ecommerce.order.domain.cart.dto.response;

import com.example.ecommerce.order.domain.cart.entity.CartItem;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CartItemResponse {

    private Long id;
    private Long cartId;
    private Long productId;
    private Long productOptionId;
    private String productName;
    private String optionName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private Integer discountRate;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;
    private Long sellerId;
    private Boolean isSelected;

    public static CartItemResponse from(CartItem cartItem) {
        BigDecimal totalPrice = cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
        BigDecimal discountAmount = totalPrice.multiply(BigDecimal.valueOf(cartItem.getDiscountRate()))
                .divide(BigDecimal.valueOf(100));
        BigDecimal finalPrice = totalPrice.subtract(discountAmount);

        return CartItemResponse.builder()
                .id(cartItem.getId())
                .cartId(cartItem.getCartId())
                .productId(cartItem.getProductId())
                .productOptionId(cartItem.getProductOptionId())
                .productName(cartItem.getProductName())
                .optionName(cartItem.getOptionName())
                .quantity(cartItem.getQuantity())
                .unitPrice(cartItem.getUnitPrice())
                .discountRate(cartItem.getDiscountRate())
                .discountAmount(discountAmount)
                .finalPrice(finalPrice)
                .sellerId(cartItem.getSellerId())
                .isSelected(cartItem.getIsSelected())
                .build();
    }
}
