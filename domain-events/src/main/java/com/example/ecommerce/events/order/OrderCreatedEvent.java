package com.example.ecommerce.events.order;

import com.example.ecommerce.events.DomainEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;

/**
 * 주문 생성 이벤트
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderCreatedEvent extends DomainEvent {

    private Long orderId;
    private Long memberId;
    private String orderNumber;
    private BigDecimal totalAmount;
    private List<OrderItem> items;

    @Getter
    @SuperBuilder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class OrderItem {
        private Long productOptionId;
        private int quantity;
        private BigDecimal price;
    }

    public static OrderCreatedEvent of(Long orderId, Long memberId, String orderNumber,
                                        BigDecimal totalAmount, List<OrderItem> items) {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(orderId)
                .memberId(memberId)
                .orderNumber(orderNumber)
                .totalAmount(totalAmount)
                .items(items)
                .build();
        event.init(String.valueOf(orderId), "Order");
        return event;
    }

    @Override
    public String getEventType() {
        return "ORDER_CREATED";
    }
}
