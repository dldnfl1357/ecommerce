package com.example.ecommerce.order.domain.order.entity;

import com.example.ecommerce.common.core.entity.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("order_items")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class OrderItem extends BaseEntity {

    @Id
    private Long id;

    @Column("order_id")
    private Long orderId;

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

    @Column("discount_amount")
    private BigDecimal discountAmount;

    @Column("final_price")
    private BigDecimal finalPrice;

    @Column("status")
    private OrderItemStatus status;

    @Column("seller_id")
    private Long sellerId;

    public BigDecimal calculateFinalPrice() {
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal discount = totalPrice.multiply(BigDecimal.valueOf(discountRate)).divide(BigDecimal.valueOf(100));
        return totalPrice.subtract(discount);
    }

    public OrderItem cancel() {
        this.status = OrderItemStatus.CANCELLED;
        return this;
    }

    public static OrderItem create(Long orderId, Long productId, Long productOptionId,
                                   String productName, String optionName, Integer quantity,
                                   BigDecimal unitPrice, Integer discountRate, Long sellerId) {
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal discountAmount = totalPrice.multiply(BigDecimal.valueOf(discountRate))
                .divide(BigDecimal.valueOf(100));
        BigDecimal finalPrice = totalPrice.subtract(discountAmount);

        return OrderItem.builder()
                .orderId(orderId)
                .productId(productId)
                .productOptionId(productOptionId)
                .productName(productName)
                .optionName(optionName)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .discountRate(discountRate)
                .discountAmount(discountAmount)
                .finalPrice(finalPrice)
                .status(OrderItemStatus.ORDERED)
                .sellerId(sellerId)
                .build();
    }
}
