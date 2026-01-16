package com.example.ecommerce.order.domain.order.dto.response;

import com.example.ecommerce.order.domain.order.entity.OrderItem;
import com.example.ecommerce.order.domain.order.entity.OrderItemStatus;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class OrderItemResponse {

    private Long id;
    private Long orderId;
    private Long productId;
    private Long productOptionId;
    private String productName;
    private String optionName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private Integer discountRate;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;
    private OrderItemStatus status;
    private String statusDescription;
    private Long sellerId;

    public static OrderItemResponse from(OrderItem orderItem) {
        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .orderId(orderItem.getOrderId())
                .productId(orderItem.getProductId())
                .productOptionId(orderItem.getProductOptionId())
                .productName(orderItem.getProductName())
                .optionName(orderItem.getOptionName())
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .discountRate(orderItem.getDiscountRate())
                .discountAmount(orderItem.getDiscountAmount())
                .finalPrice(orderItem.getFinalPrice())
                .status(orderItem.getStatus())
                .statusDescription(orderItem.getStatus().getDescription())
                .sellerId(orderItem.getSellerId())
                .build();
    }
}
