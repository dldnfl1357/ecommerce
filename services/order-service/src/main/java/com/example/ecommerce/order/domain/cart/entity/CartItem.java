package com.example.ecommerce.order.domain.cart.entity;

import com.example.ecommerce.common.core.entity.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("cart_items")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CartItem extends BaseEntity {

    @Id
    private Long id;

    @Column("cart_id")
    private Long cartId;

    @Column("product_id")
    private Long productId;

    @Column("product_option_id")
    private Long productOptionId;

    @Column("product_name")
    private String productName;

    @Column("option_name")
    private String optionName;

    @Column("quantity")
    private Integer quantity;

    @Column("unit_price")
    private BigDecimal unitPrice;

    @Column("discount_rate")
    private Integer discountRate;

    @Column("seller_id")
    private Long sellerId;

    @Column("is_selected")
    private Boolean isSelected;

    public CartItem updateQuantity(Integer quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        }
        this.quantity = quantity;
        return this;
    }

    public CartItem select() {
        this.isSelected = true;
        return this;
    }

    public CartItem deselect() {
        this.isSelected = false;
        return this;
    }

    public BigDecimal calculatePrice() {
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal discount = totalPrice.multiply(BigDecimal.valueOf(discountRate))
                .divide(BigDecimal.valueOf(100));
        return totalPrice.subtract(discount);
    }

    public static CartItem create(Long cartId, Long productId, Long productOptionId,
                                  String productName, String optionName, Integer quantity,
                                  BigDecimal unitPrice, Integer discountRate, Long sellerId) {
        return CartItem.builder()
                .cartId(cartId)
                .productId(productId)
                .productOptionId(productOptionId)
                .productName(productName)
                .optionName(optionName)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .discountRate(discountRate)
                .sellerId(sellerId)
                .isSelected(true)
                .build();
    }
}
