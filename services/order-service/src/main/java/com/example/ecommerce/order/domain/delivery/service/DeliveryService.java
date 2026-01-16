package com.example.ecommerce.order.domain.delivery.service;

import com.example.ecommerce.common.core.exception.BusinessException;
import com.example.ecommerce.common.core.exception.ErrorCode;
import com.example.ecommerce.order.domain.delivery.dto.request.DeliveryCreateRequest;
import com.example.ecommerce.order.domain.delivery.dto.request.DeliveryShipRequest;
import com.example.ecommerce.order.domain.delivery.dto.response.DeliveryHistoryResponse;
import com.example.ecommerce.order.domain.delivery.dto.response.DeliveryResponse;
import com.example.ecommerce.order.domain.delivery.entity.Delivery;
import com.example.ecommerce.order.domain.delivery.entity.DeliveryHistory;
import com.example.ecommerce.order.domain.delivery.entity.DeliveryStatus;
import com.example.ecommerce.order.domain.delivery.repository.DeliveryHistoryRepository;
import com.example.ecommerce.order.domain.delivery.repository.DeliveryRepository;
import com.example.ecommerce.order.domain.order.entity.OrderStatus;
import com.example.ecommerce.order.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryHistoryRepository deliveryHistoryRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public Mono<DeliveryResponse> createDelivery(DeliveryCreateRequest request) {
        return orderRepository.findById(request.getOrderId())
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.ORDER_NOT_FOUND)))
                .flatMap(order -> {
                    if (order.getStatus() != OrderStatus.PAID) {
                        return Mono.error(new BusinessException(ErrorCode.INVALID_ORDER_STATUS));
                    }
                    return deliveryRepository.findByOrderId(order.getId())
                            .flatMap(existing -> Mono.<Delivery>error(new BusinessException(ErrorCode.DELIVERY_ALREADY_EXISTS)))
                            .switchIfEmpty(Mono.defer(() -> {
                                Delivery delivery = Delivery.create(
                                        request.getOrderId(),
                                        request.getType(),
                                        request.getRecipientName(),
                                        request.getRecipientPhone(),
                                        request.getPostalCode(),
                                        request.getAddress(),
                                        request.getAddressDetail(),
                                        request.getDeliveryRequest()
                                );
                                return deliveryRepository.save(delivery);
                            }));
                })
                .flatMap(delivery -> createHistory(delivery, "주문 접수", "배송 준비 시작")
                        .thenReturn(delivery))
                .map(DeliveryResponse::from)
                .doOnSuccess(response -> log.info("배송 생성: deliveryId={}, orderId={}",
                        response.getId(), request.getOrderId()));
    }

    @Transactional
    public Mono<DeliveryResponse> prepareDelivery(Long deliveryId) {
        return deliveryRepository.findById(deliveryId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.DELIVERY_NOT_FOUND)))
                .map(Delivery::markAsPreparing)
                .flatMap(deliveryRepository::save)
                .flatMap(delivery -> orderRepository.findById(delivery.getOrderId())
                        .flatMap(order -> {
                            order.markAsShipped();
                            return orderRepository.save(order);
                        })
                        .thenReturn(delivery))
                .flatMap(delivery -> createHistory(delivery, "물류센터", "상품 출고 준비중")
                        .thenReturn(delivery))
                .map(DeliveryResponse::from)
                .doOnSuccess(response -> log.info("배송 준비: deliveryId={}", deliveryId));
    }

    @Transactional
    public Mono<DeliveryResponse> shipDelivery(Long deliveryId, DeliveryShipRequest request) {
        return deliveryRepository.findById(deliveryId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.DELIVERY_NOT_FOUND)))
                .map(delivery -> delivery.ship(
                        request.getCarrierCode(),
                        request.getCarrierName(),
                        request.getTrackingNumber()
                ))
                .flatMap(deliveryRepository::save)
                .flatMap(delivery -> createHistory(delivery, "물류센터", "배송 시작 - 송장번호: " + request.getTrackingNumber())
                        .thenReturn(delivery))
                .map(DeliveryResponse::from)
                .doOnSuccess(response -> log.info("배송 출발: deliveryId={}, trackingNumber={}",
                        deliveryId, request.getTrackingNumber()));
    }

    @Transactional
    public Mono<DeliveryResponse> updateDeliveryStatus(Long deliveryId, DeliveryStatus status, String location, String description) {
        return deliveryRepository.findById(deliveryId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.DELIVERY_NOT_FOUND)))
                .map(delivery -> {
                    switch (status) {
                        case OUT_FOR_DELIVERY -> delivery.markAsOutForDelivery();
                        case DELIVERED -> delivery.markAsDelivered();
                        default -> throw new BusinessException(ErrorCode.INVALID_DELIVERY_STATUS);
                    }
                    return delivery;
                })
                .flatMap(deliveryRepository::save)
                .flatMap(delivery -> {
                    if (status == DeliveryStatus.DELIVERED) {
                        return orderRepository.findById(delivery.getOrderId())
                                .flatMap(order -> {
                                    order.markAsDelivered();
                                    return orderRepository.save(order);
                                })
                                .thenReturn(delivery);
                    }
                    return Mono.just(delivery);
                })
                .flatMap(delivery -> createHistory(delivery, location, description)
                        .thenReturn(delivery))
                .map(DeliveryResponse::from)
                .doOnSuccess(response -> log.info("배송 상태 변경: deliveryId={}, status={}",
                        deliveryId, status));
    }

    @Transactional
    public Mono<DeliveryResponse> requestReturn(Long deliveryId, String reason) {
        return deliveryRepository.findById(deliveryId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.DELIVERY_NOT_FOUND)))
                .flatMap(delivery -> {
                    if (!delivery.canReturn()) {
                        return Mono.error(new BusinessException(ErrorCode.RETURN_NOT_ALLOWED));
                    }
                    return Mono.just(delivery.requestReturn(reason));
                })
                .flatMap(deliveryRepository::save)
                .flatMap(delivery -> createHistory(delivery, "고객", "반품 요청: " + reason)
                        .thenReturn(delivery))
                .map(DeliveryResponse::from)
                .doOnSuccess(response -> log.info("반품 요청: deliveryId={}", deliveryId));
    }

    public Mono<DeliveryResponse> getDelivery(Long deliveryId) {
        return deliveryRepository.findById(deliveryId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.DELIVERY_NOT_FOUND)))
                .flatMap(this::enrichWithHistories);
    }

    public Mono<DeliveryResponse> getDeliveryByOrderId(Long orderId) {
        return deliveryRepository.findByOrderId(orderId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.DELIVERY_NOT_FOUND)))
                .flatMap(this::enrichWithHistories);
    }

    public Mono<DeliveryResponse> trackDelivery(String trackingNumber) {
        return deliveryRepository.findByTrackingNumber(trackingNumber)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.DELIVERY_NOT_FOUND)))
                .flatMap(this::enrichWithHistories);
    }

    private Mono<DeliveryResponse> enrichWithHistories(Delivery delivery) {
        return deliveryHistoryRepository.findByDeliveryIdOrderByOccurredAtDesc(delivery.getId())
                .map(DeliveryHistoryResponse::from)
                .collectList()
                .map(histories -> DeliveryResponse.from(delivery, histories));
    }

    private Mono<DeliveryHistory> createHistory(Delivery delivery, String location, String description) {
        DeliveryHistory history = DeliveryHistory.create(
                delivery.getId(),
                delivery.getStatus(),
                location,
                description
        );
        return deliveryHistoryRepository.save(history);
    }
}
