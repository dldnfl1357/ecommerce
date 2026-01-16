package com.example.ecommerce.order.domain.order.entity;

import com.example.ecommerce.common.core.entity.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table("orders")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Order extends BaseEntity {

    @Id
    private Long id;

    @Column("order_number")
    private String orderNumber;

    @Column("member_id")
    private Long memberId;

    @Column("address_id")
    private Long addressId;

    @Column("total_amount")
    private BigDecimal totalAmount;

    @Column("discount_amount")
    private BigDecimal discountAmount;

    @Column("delivery_fee")
    private BigDecimal deliveryFee;

    @Column("final_amount")
    private BigDecimal finalAmount;

    @Column("point_used")
    private Integer pointUsed;

    @Column("point_earned")
    private Integer pointEarned;

    @Column("status")
    private OrderStatus status;

    @Column("ordered_at")
    private LocalDateTime orderedAt;

    @Column("paid_at")
    private LocalDateTime paidAt;

    @Column("shipped_at")
    private LocalDateTime shippedAt;

    @Column("delivered_at")
    private LocalDateTime deliveredAt;

    @Column("cancelled_at")
    private LocalDateTime cancelledAt;

    @Column("cancel_reason")
    private String cancelReason;

    public Order markAsPaid() {
        this.status = OrderStatus.PAID;
        this.paidAt = LocalDateTime.now();
        return this;
    }

    public Order markAsShipped() {
        this.status = OrderStatus.SHIPPED;
        this.shippedAt = LocalDateTime.now();
        return this;
    }

    public Order markAsDelivered() {
        this.status = OrderStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
        return this;
    }

    public Order markAsCompleted() {
        this.status = OrderStatus.COMPLETED;
        return this;
    }

    public Order cancel(String reason) {
        if (!canCancel()) {
            throw new IllegalStateException("취소할 수 없는 주문 상태입니다.");
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancelReason = reason;
        return this;
    }

    public boolean canCancel() {
        return this.status == OrderStatus.PENDING ||
               this.status == OrderStatus.PAID ||
               this.status == OrderStatus.PREPARING;
    }

    public boolean canRefund() {
        return this.status == OrderStatus.DELIVERED ||
               this.status == OrderStatus.COMPLETED;
    }

    public static Order create(Long memberId, Long addressId, BigDecimal totalAmount,
                                BigDecimal discountAmount, BigDecimal deliveryFee,
                                Integer pointUsed, String orderNumber) {
        BigDecimal finalAmount = totalAmount
                .subtract(discountAmount)
                .add(deliveryFee)
                .subtract(BigDecimal.valueOf(pointUsed));

        int pointEarned = finalAmount.multiply(BigDecimal.valueOf(0.01)).intValue();

        return Order.builder()
                .orderNumber(orderNumber)
                .memberId(memberId)
                .addressId(addressId)
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .deliveryFee(deliveryFee)
                .finalAmount(finalAmount)
                .pointUsed(pointUsed)
                .pointEarned(pointEarned)
                .status(OrderStatus.PENDING)
                .orderedAt(LocalDateTime.now())
                .build();
    }
}
