package com.example.ecommerce.order.event;

import com.example.ecommerce.events.inventory.StockReleasedEvent;
import com.example.ecommerce.events.inventory.StockReservedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventConsumer {

    @KafkaListener(topics = "inventory-events", groupId = "order-service-group")
    public void handleInventoryEvent(ConsumerRecord<String, Object> record) {
        Object event = record.value();

        if (event instanceof StockReservedEvent stockReserved) {
            handleStockReserved(stockReserved);
        } else if (event instanceof StockReleasedEvent stockReleased) {
            handleStockReleased(stockReleased);
        } else {
            log.warn("Unknown inventory event type: {}", event.getClass().getName());
        }
    }

    private void handleStockReserved(StockReservedEvent event) {
        log.info("재고 예약 이벤트 수신: productOptionId={}, orderId={}, quantity={}",
                event.getProductOptionId(), event.getOrderId(), event.getQuantity());
        // Additional processing if needed (e.g., update order status)
    }

    private void handleStockReleased(StockReleasedEvent event) {
        log.info("재고 해제 이벤트 수신: productOptionId={}, orderId={}, quantity={}",
                event.getProductOptionId(), event.getOrderId(), event.getQuantity());
        // Additional processing if needed
    }
}
