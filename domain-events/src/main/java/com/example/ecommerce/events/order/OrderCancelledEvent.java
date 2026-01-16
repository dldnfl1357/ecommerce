package com.example.ecommerce.events.order;

import com.example.ecommerce.events.DomainEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 주문 취소 이벤트
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderCancelledEvent extends DomainEvent {

    private Long orderId;
    private Long memberId;
    private String orderNumber;
    private String reason;
    private List<CancelledItem> items;

    @Getter
    @SuperBuilder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class CancelledItem {
        private Long productOptionId;
        private int quantity;
        private String reservationId;
    }

    public static OrderCancelledEvent of(Long orderId, Long memberId, String orderNumber,
                                          String reason, List<CancelledItem> items) {
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(orderId)
                .memberId(memberId)
                .orderNumber(orderNumber)
                .reason(reason)
                .items(items)
                .build();
        event.init(String.valueOf(orderId), "Order");
        return event;
    }

    @Override
    public String getEventType() {
        return "ORDER_CANCELLED";
    }
}
