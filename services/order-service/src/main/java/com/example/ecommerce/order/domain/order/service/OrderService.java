package com.example.ecommerce.order.domain.order.service;

import com.example.ecommerce.common.core.exception.BusinessException;
import com.example.ecommerce.common.core.exception.ErrorCode;
import com.example.ecommerce.common.kafka.Topics;
import com.example.ecommerce.common.kafka.publisher.EventPublisher;
import com.example.ecommerce.events.order.OrderCancelledEvent;
import com.example.ecommerce.events.order.OrderCreatedEvent;
import com.example.ecommerce.order.domain.order.dto.request.OrderCancelRequest;
import com.example.ecommerce.order.domain.order.dto.request.OrderCreateRequest;
import com.example.ecommerce.order.domain.order.dto.response.OrderItemResponse;
import com.example.ecommerce.order.domain.order.dto.response.OrderResponse;
import com.example.ecommerce.order.domain.order.entity.Order;
import com.example.ecommerce.order.domain.order.entity.OrderItem;
import com.example.ecommerce.order.domain.order.entity.OrderStatus;
import com.example.ecommerce.order.domain.order.repository.OrderItemRepository;
import com.example.ecommerce.order.domain.order.repository.OrderRepository;
import com.example.ecommerce.order.external.ProductServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductServiceClient productServiceClient;
    private final EventPublisher eventPublisher;

    private static final BigDecimal FREE_DELIVERY_THRESHOLD = BigDecimal.valueOf(30000);
    private static final BigDecimal DELIVERY_FEE = BigDecimal.valueOf(3000);

    @Transactional
    public Mono<OrderResponse> createOrder(Long memberId, OrderCreateRequest request) {
        String orderNumber = generateOrderNumber();

        return Flux.fromIterable(request.getItems())
                .flatMap(item -> productServiceClient.reserveStock(
                        item.getProductOptionId(),
                        item.getQuantity(),
                        null
                ).map(inventory -> item))
                .collectList()
                .flatMap(items -> {
                    BigDecimal totalAmount = BigDecimal.ZERO;
                    BigDecimal deliveryFee = totalAmount.compareTo(FREE_DELIVERY_THRESHOLD) >= 0
                            ? BigDecimal.ZERO : DELIVERY_FEE;
                    Integer pointToUse = request.getPointToUse() != null ? request.getPointToUse() : 0;

                    Order order = Order.create(
                            memberId,
                            request.getAddressId(),
                            totalAmount,
                            BigDecimal.ZERO,
                            deliveryFee,
                            pointToUse,
                            orderNumber
                    );

                    return orderRepository.save(order);
                })
                .flatMap(savedOrder -> {
                    List<OrderItem> orderItems = request.getItems().stream()
                            .map(item -> OrderItem.create(
                                    savedOrder.getId(),
                                    item.getProductId(),
                                    item.getProductOptionId(),
                                    "상품명",
                                    "옵션명",
                                    item.getQuantity(),
                                    BigDecimal.ZERO,
                                    0,
                                    null
                            ))
                            .collect(Collectors.toList());

                    return orderItemRepository.saveAll(orderItems)
                            .collectList()
                            .map(savedItems -> {
                                List<OrderItemResponse> itemResponses = savedItems.stream()
                                        .map(OrderItemResponse::from)
                                        .collect(Collectors.toList());
                                return OrderResponse.from(savedOrder, itemResponses);
                            });
                })
                .flatMap(response -> {
                    OrderCreatedEvent event = OrderCreatedEvent.of(
                            response.getId(),
                            response.getOrderNumber(),
                            response.getMemberId(),
                            response.getFinalAmount()
                    );
                    return eventPublisher.publish(Topics.ORDER_EVENTS, event)
                            .thenReturn(response);
                })
                .doOnSuccess(response -> log.info("주문 생성 완료: orderId={}, orderNumber={}",
                        response.getId(), response.getOrderNumber()));
    }

    public Mono<OrderResponse> getOrder(Long orderId, Long memberId) {
        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.ORDER_NOT_FOUND)))
                .filter(order -> order.getMemberId().equals(memberId))
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.ACCESS_DENIED)))
                .flatMap(this::enrichOrderWithItems);
    }

    public Mono<OrderResponse> getOrderByOrderNumber(String orderNumber, Long memberId) {
        return orderRepository.findByOrderNumber(orderNumber)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.ORDER_NOT_FOUND)))
                .filter(order -> order.getMemberId().equals(memberId))
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.ACCESS_DENIED)))
                .flatMap(this::enrichOrderWithItems);
    }

    public Flux<OrderResponse> getMyOrders(Long memberId) {
        return orderRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
                .flatMap(this::enrichOrderWithItems);
    }

    public Flux<OrderResponse> getMyOrdersByStatus(Long memberId, OrderStatus status) {
        return orderRepository.findByMemberIdAndStatusOrderByCreatedAtDesc(memberId, status)
                .flatMap(this::enrichOrderWithItems);
    }

    @Transactional
    public Mono<OrderResponse> cancelOrder(Long orderId, Long memberId, OrderCancelRequest request) {
        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.ORDER_NOT_FOUND)))
                .filter(order -> order.getMemberId().equals(memberId))
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.ACCESS_DENIED)))
                .flatMap(order -> {
                    if (!order.canCancel()) {
                        return Mono.error(new BusinessException(ErrorCode.ORDER_CANNOT_CANCEL));
                    }
                    return Mono.just(order.cancel(request.getReason()));
                })
                .flatMap(orderRepository::save)
                .flatMap(order -> orderItemRepository.findByOrderId(order.getId())
                        .flatMap(item -> productServiceClient.releaseStock(
                                item.getProductOptionId(),
                                item.getQuantity(),
                                order.getId(),
                                "주문 취소"
                        ).thenReturn(item))
                        .collectList()
                        .map(items -> order))
                .flatMap(order -> {
                    OrderCancelledEvent event = OrderCancelledEvent.of(
                            order.getId(),
                            order.getOrderNumber(),
                            order.getMemberId(),
                            request.getReason()
                    );
                    return eventPublisher.publish(Topics.ORDER_EVENTS, event)
                            .thenReturn(order);
                })
                .flatMap(this::enrichOrderWithItems)
                .doOnSuccess(response -> log.info("주문 취소 완료: orderId={}", orderId));
    }

    @Transactional
    public Mono<OrderResponse> updateOrderStatus(Long orderId, OrderStatus newStatus) {
        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.ORDER_NOT_FOUND)))
                .map(order -> {
                    switch (newStatus) {
                        case PAID -> order.markAsPaid();
                        case SHIPPED -> order.markAsShipped();
                        case DELIVERED -> order.markAsDelivered();
                        case COMPLETED -> order.markAsCompleted();
                        default -> throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
                    }
                    return order;
                })
                .flatMap(orderRepository::save)
                .flatMap(this::enrichOrderWithItems)
                .doOnSuccess(response -> log.info("주문 상태 변경: orderId={}, newStatus={}",
                        orderId, newStatus));
    }

    private Mono<OrderResponse> enrichOrderWithItems(Order order) {
        return orderItemRepository.findByOrderId(order.getId())
                .map(OrderItemResponse::from)
                .collectList()
                .map(items -> OrderResponse.from(order, items));
    }

    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD" + timestamp + uuid;
    }
}
