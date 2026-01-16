package com.example.ecommerce.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.example.ecommerce.events.member.MemberCreatedEvent;
import com.example.ecommerce.events.member.MemberWithdrawnEvent;
import com.example.ecommerce.events.inventory.StockReservedEvent;
import com.example.ecommerce.events.inventory.StockReleasedEvent;
import com.example.ecommerce.events.order.OrderCreatedEvent;
import com.example.ecommerce.events.order.OrderCancelledEvent;
import com.example.ecommerce.events.order.OrderCompletedEvent;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 도메인 이벤트 기본 클래스
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
        // Member Events
        @JsonSubTypes.Type(value = MemberCreatedEvent.class, name = "MEMBER_CREATED"),
        @JsonSubTypes.Type(value = MemberWithdrawnEvent.class, name = "MEMBER_WITHDRAWN"),
        // Inventory Events
        @JsonSubTypes.Type(value = StockReservedEvent.class, name = "STOCK_RESERVED"),
        @JsonSubTypes.Type(value = StockReleasedEvent.class, name = "STOCK_RELEASED"),
        // Order Events
        @JsonSubTypes.Type(value = OrderCreatedEvent.class, name = "ORDER_CREATED"),
        @JsonSubTypes.Type(value = OrderCancelledEvent.class, name = "ORDER_CANCELLED"),
        @JsonSubTypes.Type(value = OrderCompletedEvent.class, name = "ORDER_COMPLETED")
})
public abstract class DomainEvent {

    private String eventId;
    private String aggregateId;
    private String aggregateType;
    private LocalDateTime occurredAt;
    private int version;

    protected void init(String aggregateId, String aggregateType) {
        this.eventId = UUID.randomUUID().toString();
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.occurredAt = LocalDateTime.now();
        this.version = 1;
    }

    public abstract String getEventType();
}
