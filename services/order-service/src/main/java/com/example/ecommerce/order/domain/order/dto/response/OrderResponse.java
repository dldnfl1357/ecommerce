package com.example.ecommerce.order.domain.order.dto.response;

import com.example.ecommerce.order.domain.order.entity.Order;
import com.example.ecommerce.order.domain.order.entity.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private String orderNumber;
    private Long memberId;
    private Long addressId;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal deliveryFee;
    private BigDecimal finalAmount;
    private Integer pointUsed;
    private Integer pointEarned;
    private OrderStatus status;
    private String statusDescription;
    private LocalDateTime orderedAt;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private List<OrderItemResponse> items;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .memberId(order.getMemberId())
                .addressId(order.getAddressId())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .deliveryFee(order.getDeliveryFee())
                .finalAmount(order.getFinalAmount())
                .pointUsed(order.getPointUsed())
                .pointEarned(order.getPointEarned())
                .status(order.getStatus())
                .statusDescription(order.getStatus().getDescription())
                .orderedAt(order.getOrderedAt())
                .paidAt(order.getPaidAt())
                .shippedAt(order.getShippedAt())
                .deliveredAt(order.getDeliveredAt())
                .cancelledAt(order.getCancelledAt())
                .cancelReason(order.getCancelReason())
                .build();
    }

    public static OrderResponse from(Order order, List<OrderItemResponse> items) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .memberId(order.getMemberId())
                .addressId(order.getAddressId())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .deliveryFee(order.getDeliveryFee())
                .finalAmount(order.getFinalAmount())
                .pointUsed(order.getPointUsed())
                .pointEarned(order.getPointEarned())
                .status(order.getStatus())
                .statusDescription(order.getStatus().getDescription())
                .orderedAt(order.getOrderedAt())
                .paidAt(order.getPaidAt())
                .shippedAt(order.getShippedAt())
                .deliveredAt(order.getDeliveredAt())
                .cancelledAt(order.getCancelledAt())
                .cancelReason(order.getCancelReason())
                .items(items)
                .build();
    }
}
