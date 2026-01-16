package com.example.ecommerce.events.inventory;

import com.example.ecommerce.events.DomainEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 재고 해제 이벤트
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockReleasedEvent extends DomainEvent {

    private Long productOptionId;
    private Long orderId;
    private int quantity;
    private String reservationId;
    private String reason;

    public static StockReleasedEvent of(Long productOptionId, Long orderId, int quantity,
                                         String reservationId, String reason) {
        StockReleasedEvent event = StockReleasedEvent.builder()
                .productOptionId(productOptionId)
                .orderId(orderId)
                .quantity(quantity)
                .reservationId(reservationId)
                .reason(reason)
                .build();
        event.init(String.valueOf(productOptionId), "Inventory");
        return event;
    }

    @Override
    public String getEventType() {
        return "STOCK_RELEASED";
    }
}
