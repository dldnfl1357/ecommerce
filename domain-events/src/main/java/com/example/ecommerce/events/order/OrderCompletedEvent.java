package com.example.ecommerce.events.order;

import com.example.ecommerce.events.DomainEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * 주문 완료 이벤트
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderCompletedEvent extends DomainEvent {

    private Long orderId;
    private Long memberId;
    private String orderNumber;
    private BigDecimal totalAmount;
    private int earnedPoints;

    public static OrderCompletedEvent of(Long orderId, Long memberId, String orderNumber,
                                          BigDecimal totalAmount, int earnedPoints) {
        OrderCompletedEvent event = OrderCompletedEvent.builder()
                .orderId(orderId)
                .memberId(memberId)
                .orderNumber(orderNumber)
                .totalAmount(totalAmount)
                .earnedPoints(earnedPoints)
                .build();
        event.init(String.valueOf(orderId), "Order");
        return event;
    }

    @Override
    public String getEventType() {
        return "ORDER_COMPLETED";
    }
}
