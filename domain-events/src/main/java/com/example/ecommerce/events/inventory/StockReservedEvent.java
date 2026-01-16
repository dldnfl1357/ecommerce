package com.example.ecommerce.events.inventory;

import com.example.ecommerce.events.DomainEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 재고 예약 이벤트
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockReservedEvent extends DomainEvent {

    private Long productOptionId;
    private Long orderId;
    private int quantity;
    private String reservationId;

    public static StockReservedEvent of(Long productOptionId, Long orderId, int quantity, String reservationId) {
        StockReservedEvent event = StockReservedEvent.builder()
                .productOptionId(productOptionId)
                .orderId(orderId)
                .quantity(quantity)
                .reservationId(reservationId)
                .build();
        event.init(String.valueOf(productOptionId), "Inventory");
        return event;
    }

    @Override
    public String getEventType() {
        return "STOCK_RESERVED";
    }
}
