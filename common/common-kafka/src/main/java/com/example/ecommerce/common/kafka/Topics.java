package com.example.ecommerce.common.kafka;

/**
 * Kafka 토픽 상수 정의
 */
public final class Topics {

    private Topics() {
        // Utility class
    }

    // Member Service Topics
    public static final String MEMBER_EVENTS = "member-events";

    // Product Service Topics
    public static final String PRODUCT_EVENTS = "product-events";
    public static final String INVENTORY_EVENTS = "inventory-events";

    // Order Service Topics
    public static final String ORDER_EVENTS = "order-events";
    public static final String PAYMENT_EVENTS = "payment-events";
}
