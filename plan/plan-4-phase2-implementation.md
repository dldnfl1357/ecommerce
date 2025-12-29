# 쿠팡 클론 프로젝트 기획서 (4/6) - Phase 2 구현 가이드

> **핵심 비즈니스 로직 구현: Order, Payment, Delivery, Search (Week 9-16)**

---

## 📋 목차
1. [Week 9-10: Order Service](#week-9-10-order-service)
2. [Week 11-12: Payment Service](#week-11-12-payment-service)
3. [Week 13-14: Delivery Service](#week-13-14-delivery-service)
4. [Week 15-16: Search Service](#week-15-16-search-service)

---

## 📝 Week 9-10: Order Service

### Day 1-2: 주문 생성 로직 구현

#### 1.1 OrderService - 주문 생성

**OrderService.java**:
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final ProductOptionRepository productOptionRepository;
    private final InventoryService inventoryService;
    private final CouponService couponService;
    private final OrderEventPublisher eventPublisher;

    /**
     * 주문 생성
     * 1. 주문 유효성 검증
     * 2. 재고 예약
     * 3. 주문 생성
     * 4. 쿠폰/적립금 적용
     * 5. 이벤트 발행
     */
    @Transactional
    public OrderResponse createOrder(Long memberId, OrderCreateRequest request) {
        // 1. 회원 조회
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberNotFoundException(memberId));

        // 2. 상품 옵션 조회 및 검증
        List<OrderItemCreateRequest> itemRequests = request.getItems();
        List<ProductOption> options = validateAndGetProductOptions(itemRequests);

        // 3. 재고 예약 (트랜잭션 내에서 순차적으로)
        try {
            for (OrderItemCreateRequest itemReq : itemRequests) {
                inventoryService.reserve(
                    itemReq.getProductOptionId(),
                    itemReq.getQuantity()
                );
            }
        } catch (InsufficientStockException e) {
            // 예약 실패 시 롤백
            throw new OrderCreationException("재고 부족으로 주문 실패", e);
        }

        // 4. 주문 생성
        Order order = Order.create(
            member,
            itemRequests.stream()
                .map(req -> {
                    ProductOption option = findOption(options, req.getProductOptionId());
                    return new OrderItemCreateRequest(option, req.getQuantity());
                })
                .collect(Collectors.toList()),
            DeliveryInfo.from(request.getDeliveryInfo())
        );

        // 5. 쿠폰 적용
        if (request.getCouponId() != null) {
            int couponDiscount = couponService.useCoupon(
                request.getCouponId(),
                memberId,
                order.getTotalProductPrice()
            );
            order.applyCoupon(couponDiscount);
        }

        // 6. 적립금 사용
        if (request.getPointToUse() > 0) {
            member.usePoint(request.getPointToUse());
            order.usePoint(request.getPointToUse());
        }

        // 7. 주문 저장
        Order savedOrder = orderRepository.save(order);

        // 8. 이벤트 발행
        eventPublisher.publishOrderCreated(savedOrder);

        return OrderResponse.from(savedOrder);
    }

    /**
     * 주문 취소
     */
    @Transactional
    public void cancelOrder(Long memberId, Long orderId) {
        // 1. 주문 조회
        Order order = orderRepository.findByIdWithItems(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        // 2. 권한 확인
        if (!order.isOwnedBy(memberId)) {
            throw new UnauthorizedOrderAccessException();
        }

        // 3. 취소 가능 여부 확인
        if (!order.isCancellable()) {
            throw new OrderNotCancellableException(order.getStatus());
        }

        // 4. 재고 복구
        for (OrderItem item : order.getOrderItems()) {
            inventoryService.cancelReservation(
                item.getProductOption().getId(),
                item.getQuantity()
            );
        }

        // 5. 쿠폰 복구
        if (order.getCouponId() != null) {
            couponService.restoreCoupon(order.getCouponId());
        }

        // 6. 적립금 복구
        if (order.getPointUsed() > 0) {
            order.getMember().earnPoint(order.getPointUsed());
        }

        // 7. 주문 취소 처리
        order.cancel();

        // 8. 이벤트 발행 (결제 취소 처리)
        eventPublisher.publishOrderCancelled(order);
    }

    /**
     * 부분 취소 (특정 주문 항목만 취소)
     */
    @Transactional
    public void cancelOrderItem(Long memberId, Long orderId, Long orderItemId) {
        // 1. 주문 조회
        Order order = orderRepository.findByIdWithItems(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        // 2. 권한 확인
        if (!order.isOwnedBy(memberId)) {
            throw new UnauthorizedOrderAccessException();
        }

        // 3. 주문 항목 찾기
        OrderItem orderItem = order.findOrderItem(orderItemId);
        if (orderItem == null) {
            throw new OrderItemNotFoundException(orderItemId);
        }

        // 4. 취소 가능 여부 확인
        if (!orderItem.isCancellable()) {
            throw new OrderItemNotCancellableException(orderItem.getStatus());
        }

        // 5. 재고 복구
        inventoryService.cancelReservation(
            orderItem.getProductOption().getId(),
            orderItem.getQuantity()
        );

        // 6. 부분 환불 금액 계산
        int refundAmount = orderItem.getTotalPrice();
        int refundDeliveryFee = calculatePartialRefundDeliveryFee(order, orderItem);
        refundAmount += refundDeliveryFee;

        // 7. 주문 항목 취소
        orderItem.cancel();

        // 8. 주문 금액 재계산
        order.recalculatePrices();

        // 9. 모든 항목이 취소되면 주문 전체 취소
        if (order.isAllItemsCancelled()) {
            order.cancel();
        }

        // 10. 이벤트 발행 (부분 환불 처리)
        eventPublisher.publishOrderItemCancelled(order, orderItem, refundAmount);
    }

    /**
     * 구매 확정
     */
    @Transactional
    public void confirmOrder(Long memberId, Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.isOwnedBy(memberId)) {
            throw new UnauthorizedOrderAccessException();
        }

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new InvalidOrderStatusException("배송 완료 상태에서만 구매 확정 가능");
        }

        // 구매 확정
        order.confirm();

        // 적립금 지급
        int earnedPoint = calculateEarnedPoint(order);
        order.getMember().earnPoint(earnedPoint);

        // 이벤트 발행 (정산 처리, 리뷰 작성 가능)
        eventPublisher.publishOrderConfirmed(order);
    }

    private int calculateEarnedPoint(Order order) {
        Member member = order.getMember();
        return (int) (order.getFinalPrice() * member.getGrade().getPointRate());
    }

    private int calculatePartialRefundDeliveryFee(Order order, OrderItem cancelledItem) {
        // 배송비 부분 환불 로직
        // 같은 판매자/배송 타입의 다른 상품이 남아있으면 환불 안함
        long remainingItemsCount = order.getOrderItems().stream()
            .filter(item -> item.getStatus() != OrderItemStatus.CANCELLED)
            .filter(item -> !item.getId().equals(cancelledItem.getId()))
            .filter(item -> isSameDeliveryGroup(item, cancelledItem))
            .count();

        if (remainingItemsCount == 0) {
            // 같은 배송 그룹의 마지막 상품이면 배송비 환불
            DeliveryType deliveryType = cancelledItem.getProductOption()
                .getProduct().getDeliveryType();
            return deliveryType.getBaseFee();
        }

        return 0;
    }

    private boolean isSameDeliveryGroup(OrderItem item1, OrderItem item2) {
        Product product1 = item1.getProductOption().getProduct();
        Product product2 = item2.getProductOption().getProduct();

        return product1.getSeller().getId().equals(product2.getSeller().getId())
            && product1.getDeliveryType() == product2.getDeliveryType();
    }
}
```

### Day 3-4: 주문 상태 관리 (Saga Pattern)

#### 2.1 주문 Saga Orchestrator

**OrderSagaOrchestrator.java**:
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSagaOrchestrator {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final MemberService memberService;
    private final CouponService couponService;

    /**
     * 주문 Saga 시작
     * 1. 재고 예약
     * 2. 결제 처리
     * 3. 재고 확정
     * 4. 주문 완료
     *
     * 실패 시 보상 트랜잭션 실행
     */
    @Transactional
    public void executeOrderSaga(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        try {
            // Step 1: 재고 예약
            log.info("Saga Step 1: 재고 예약 시작");
            reserveInventory(order);

            // Step 2: 결제 처리
            log.info("Saga Step 2: 결제 처리 시작");
            Payment payment = processPayment(order);

            // Step 3: 재고 확정
            log.info("Saga Step 3: 재고 확정 시작");
            confirmInventory(order);

            // Step 4: 주문 완료
            log.info("Saga Step 4: 주문 완료");
            order.paid();

            log.info("Order Saga completed successfully: orderId={}", orderId);

        } catch (Exception e) {
            log.error("Order Saga failed: orderId={}", orderId, e);
            compensate(order);
            throw new OrderSagaException("주문 처리 실패", e);
        }
    }

    private void reserveInventory(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            inventoryService.reserve(
                item.getProductOption().getId(),
                item.getQuantity()
            );
        }
    }

    private Payment processPayment(Order order) {
        PaymentRequest request = PaymentRequest.builder()
            .orderId(order.getId())
            .amount(order.getFinalPrice())
            .method(PaymentMethod.CARD)
            .build();

        return paymentService.processPayment(request);
    }

    private void confirmInventory(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            inventoryService.confirmReservation(
                item.getProductOption().getId(),
                item.getQuantity()
            );
        }
    }

    /**
     * 보상 트랜잭션 (Compensating Transaction)
     */
    private void compensate(Order order) {
        log.info("Starting compensation for order: {}", order.getId());

        try {
            // 1. 재고 예약 취소
            for (OrderItem item : order.getOrderItems()) {
                inventoryService.cancelReservation(
                    item.getProductOption().getId(),
                    item.getQuantity()
                );
            }

            // 2. 결제 취소
            paymentService.cancelPayment(order.getId());

            // 3. 쿠폰 복구
            if (order.getCouponId() != null) {
                couponService.restoreCoupon(order.getCouponId());
            }

            // 4. 적립금 복구
            if (order.getPointUsed() > 0) {
                memberService.earnPoint(
                    order.getMember().getId(),
                    order.getPointUsed()
                );
            }

            // 5. 주문 상태 변경
            order.cancel();

            log.info("Compensation completed for order: {}", order.getId());

        } catch (Exception e) {
            log.error("Compensation failed for order: {}", order.getId(), e);
            // 보상 실패 시 수동 처리 필요 (알림 발송)
        }
    }
}
```

### Day 5: 주문 조회 및 통계

**OrderQueryService.java**:
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final JPAQueryFactory queryFactory;

    /**
     * 내 주문 목록 조회 (페이징)
     */
    public Page<OrderResponse> getMyOrders(
        Long memberId,
        OrderSearchCondition condition,
        Pageable pageable
    ) {
        List<Order> orders = queryFactory
            .selectFrom(order)
            .where(
                order.member.id.eq(memberId),
                statusEq(condition.getStatus()),
                orderedAtBetween(condition.getStartDate(), condition.getEndDate())
            )
            .orderBy(order.orderedAt.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long total = queryFactory
            .select(order.count())
            .from(order)
            .where(
                order.member.id.eq(memberId),
                statusEq(condition.getStatus()),
                orderedAtBetween(condition.getStartDate(), condition.getEndDate())
            )
            .fetchOne();

        List<OrderResponse> responses = orders.stream()
            .map(OrderResponse::from)
            .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, total);
    }

    /**
     * 주문 상세 조회
     */
    public OrderDetailResponse getOrderDetail(Long memberId, Long orderId) {
        Order order = orderRepository.findByIdWithDetails(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.isOwnedBy(memberId)) {
            throw new UnauthorizedOrderAccessException();
        }

        return OrderDetailResponse.from(order);
    }

    /**
     * 주문 통계 조회
     */
    public OrderStatisticsResponse getOrderStatistics(
        Long memberId,
        LocalDateTime startDate,
        LocalDateTime endDate
    ) {
        // 기간별 주문 건수
        Long totalOrderCount = queryFactory
            .select(order.count())
            .from(order)
            .where(
                order.member.id.eq(memberId),
                order.orderedAt.between(startDate, endDate)
            )
            .fetchOne();

        // 기간별 총 구매 금액
        Integer totalPurchaseAmount = queryFactory
            .select(order.finalPrice.sum())
            .from(order)
            .where(
                order.member.id.eq(memberId),
                order.status.eq(OrderStatus.CONFIRMED),
                order.orderedAt.between(startDate, endDate)
            )
            .fetchOne();

        // 상태별 주문 건수
        Map<OrderStatus, Long> statusCount = queryFactory
            .select(order.status, order.count())
            .from(order)
            .where(
                order.member.id.eq(memberId),
                order.orderedAt.between(startDate, endDate)
            )
            .groupBy(order.status)
            .fetch()
            .stream()
            .collect(Collectors.toMap(
                tuple -> tuple.get(order.status),
                tuple -> tuple.get(order.count())
            ));

        return OrderStatisticsResponse.builder()
            .totalOrderCount(totalOrderCount)
            .totalPurchaseAmount(totalPurchaseAmount != null ? totalPurchaseAmount : 0)
            .statusCount(statusCount)
            .build();
    }

    private BooleanExpression statusEq(OrderStatus status) {
        return status != null ? order.status.eq(status) : null;
    }

    private BooleanExpression orderedAtBetween(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null) {
            return order.orderedAt.between(start, end);
        }
        return null;
    }
}
```

---

## 💳 Week 11-12: Payment Service

### Day 1-3: PG사 연동 (Toss Payments)

#### 3.1 Payment Service 구현

**PaymentService.java**:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final TossPaymentClient tossPaymentClient;
    private final PaymentEventPublisher eventPublisher;

    /**
     * 결제 준비 (결제 창 띄우기 전)
     */
    @Transactional
    public PaymentPrepareResponse preparePayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidOrderStatusException("결제 대기 상태가 아닙니다");
        }

        // Payment 엔티티 생성 (상태: PENDING)
        Payment payment = Payment.prepare(order);
        paymentRepository.save(payment);

        // Toss Payments에 결제 준비 요청
        String paymentKey = tossPaymentClient.prepare(
            payment.getPaymentId(),
            order.getFinalPrice()
        );

        payment.updatePaymentKey(paymentKey);

        return PaymentPrepareResponse.builder()
            .paymentId(payment.getId())
            .paymentKey(paymentKey)
            .orderId(order.getId())
            .amount(order.getFinalPrice())
            .build();
    }

    /**
     * 결제 승인 (사용자가 결제 완료 후 콜백)
     */
    @Transactional
    public PaymentResponse confirmPayment(PaymentConfirmRequest request) {
        // 1. Payment 조회
        Payment payment = paymentRepository.findByPaymentKey(request.getPaymentKey())
            .orElseThrow(() -> new PaymentNotFoundException());

        // 2. 이미 처리된 결제인지 확인 (멱등성 보장)
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            log.warn("이미 처리된 결제: paymentKey={}", request.getPaymentKey());
            return PaymentResponse.from(payment);
        }

        // 3. 주문 조회
        Order order = payment.getOrder();

        // 4. 금액 검증
        if (!payment.getAmount().equals(request.getAmount())) {
            throw new PaymentAmountMismatchException();
        }

        try {
            // 5. Toss Payments에 결제 승인 요청
            TossPaymentConfirmResponse tossResponse = tossPaymentClient.confirm(
                request.getPaymentKey(),
                request.getOrderId(),
                request.getAmount()
            );

            // 6. 결제 완료 처리
            payment.complete(
                tossResponse.getTransactionKey(),
                tossResponse.getMethod(),
                tossResponse.getApprovedAt()
            );

            // 7. 주문 상태 변경
            order.paid();

            // 8. 재고 확정 이벤트 발행
            eventPublisher.publishPaymentCompleted(payment);

            log.info("결제 승인 완료: paymentKey={}, amount={}",
                request.getPaymentKey(), request.getAmount());

            return PaymentResponse.from(payment);

        } catch (TossPaymentException e) {
            // 결제 실패 처리
            payment.fail(e.getMessage());
            log.error("결제 승인 실패: paymentKey={}", request.getPaymentKey(), e);
            throw new PaymentFailedException("결제 승인 실패", e);
        }
    }

    /**
     * 결제 취소 (전액 환불)
     */
    @Transactional
    public void cancelPayment(Long orderId, String cancelReason) {
        // 1. Payment 조회
        Payment payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new PaymentNotFoundException());

        // 2. 취소 가능 상태 확인
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new PaymentNotCancellableException();
        }

        try {
            // 3. Toss Payments에 취소 요청
            TossPaymentCancelResponse response = tossPaymentClient.cancel(
                payment.getPaymentKey(),
                cancelReason
            );

            // 4. 결제 취소 처리
            payment.cancel(response.getCancelledAt());

            // 5. 이벤트 발행
            eventPublisher.publishPaymentCancelled(payment);

            log.info("결제 취소 완료: orderId={}, amount={}",
                orderId, payment.getAmount());

        } catch (TossPaymentException e) {
            log.error("결제 취소 실패: orderId={}", orderId, e);
            throw new PaymentCancelFailedException("결제 취소 실패", e);
        }
    }

    /**
     * 부분 취소 (부분 환불)
     */
    @Transactional
    public void partialCancelPayment(
        Long orderId,
        int cancelAmount,
        String cancelReason
    ) {
        Payment payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new PaymentNotFoundException());

        if (payment.getRefundableAmount() < cancelAmount) {
            throw new ExceededRefundableAmountException();
        }

        try {
            TossPaymentCancelResponse response = tossPaymentClient.partialCancel(
                payment.getPaymentKey(),
                cancelAmount,
                cancelReason
            );

            payment.partialCancel(cancelAmount, response.getCancelledAt());

            eventPublisher.publishPaymentPartiallyCancelled(payment, cancelAmount);

            log.info("부분 환불 완료: orderId={}, cancelAmount={}",
                orderId, cancelAmount);

        } catch (TossPaymentException e) {
            log.error("부분 환불 실패: orderId={}, cancelAmount={}",
                orderId, cancelAmount, e);
            throw new PaymentCancelFailedException("부분 환불 실패", e);
        }
    }
}
```

#### 3.2 Toss Payments 클라이언트

**TossPaymentClient.java**:
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class TossPaymentClient {

    @Value("${toss.payments.secret-key}")
    private String secretKey;

    @Value("${toss.payments.api-url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 결제 승인
     */
    public TossPaymentConfirmResponse confirm(
        String paymentKey,
        String orderId,
        Integer amount
    ) {
        String url = apiUrl + "/v1/payments/confirm";

        HttpHeaders headers = createHeaders();
        Map<String, Object> body = Map.of(
            "paymentKey", paymentKey,
            "orderId", orderId,
            "amount", amount
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                url, entity, String.class
            );

            return objectMapper.readValue(
                response.getBody(),
                TossPaymentConfirmResponse.class
            );

        } catch (HttpClientErrorException e) {
            log.error("Toss Payments 결제 승인 실패: {}", e.getResponseBodyAsString());
            throw new TossPaymentException("결제 승인 실패", e);
        } catch (Exception e) {
            log.error("Toss Payments 통신 실패", e);
            throw new TossPaymentException("결제 서비스 통신 실패", e);
        }
    }

    /**
     * 결제 취소
     */
    public TossPaymentCancelResponse cancel(String paymentKey, String cancelReason) {
        String url = apiUrl + "/v1/payments/" + paymentKey + "/cancel";

        HttpHeaders headers = createHeaders();
        Map<String, String> body = Map.of("cancelReason", cancelReason);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                url, entity, String.class
            );

            return objectMapper.readValue(
                response.getBody(),
                TossPaymentCancelResponse.class
            );

        } catch (Exception e) {
            log.error("Toss Payments 결제 취소 실패", e);
            throw new TossPaymentException("결제 취소 실패", e);
        }
    }

    /**
     * 부분 취소
     */
    public TossPaymentCancelResponse partialCancel(
        String paymentKey,
        int cancelAmount,
        String cancelReason
    ) {
        String url = apiUrl + "/v1/payments/" + paymentKey + "/cancel";

        HttpHeaders headers = createHeaders();
        Map<String, Object> body = Map.of(
            "cancelAmount", cancelAmount,
            "cancelReason", cancelReason
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                url, entity, String.class
            );

            return objectMapper.readValue(
                response.getBody(),
                TossPaymentCancelResponse.class
            );

        } catch (Exception e) {
            log.error("Toss Payments 부분 취소 실패", e);
            throw new TossPaymentException("부분 취소 실패", e);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String auth = secretKey + ":";
        String encodedAuth = Base64.getEncoder()
            .encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);

        return headers;
    }
}
```

---

## 🚚 Week 13-14: Delivery Service

### Day 1-3: 배송 관리 시스템

**DeliveryService.java**:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;
    private final DeliveryTrackerClient trackerClient;
    private final DeliveryEventPublisher eventPublisher;

    /**
     * 배송 시작
     */
    @Transactional
    public DeliveryResponse startDelivery(
        Long orderId,
        DeliveryStartRequest request
    ) {
        // 1. 주문 조회
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        // 2. 주문 상태 확인
        if (order.getStatus() != OrderStatus.PREPARING) {
            throw new InvalidOrderStatusException("상품 준비중 상태가 아닙니다");
        }

        // 3. 배송 정보 생성
        Delivery delivery = Delivery.builder()
            .order(order)
            .carrier(request.getCarrier())
            .trackingNumber(request.getTrackingNumber())
            .status(DeliveryStatus.PICKED_UP)
            .deliveryInfo(order.getDeliveryInfo())
            .deliveryFee(order.getTotalDeliveryFee())
            .build();

        deliveryRepository.save(delivery);

        // 4. 주문 상태 변경
        order.shipping();

        // 5. 배송 추적 이력 추가
        delivery.addHistory(
            DeliveryStatus.PICKED_UP,
            "물류센터",
            "상품이 물류센터에서 출고되었습니다"
        );

        // 6. 이벤트 발행 (알림)
        eventPublisher.publishDeliveryStarted(delivery);

        return DeliveryResponse.from(delivery);
    }

    /**
     * 배송 상태 업데이트
     */
    @Transactional
    public void updateDeliveryStatus(
        Long deliveryId,
        DeliveryStatusUpdateRequest request
    ) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow(() -> new DeliveryNotFoundException(deliveryId));

        // 배송 상태 변경
        delivery.updateStatus(request.getStatus());

        // 이력 추가
        delivery.addHistory(
            request.getStatus(),
            request.getLocation(),
            request.getDescription()
        );

        // 배송 완료 시 주문 상태 변경
        if (request.getStatus() == DeliveryStatus.DELIVERED) {
            delivery.getOrder().delivered();
            delivery.complete();
        }

        // 이벤트 발행
        eventPublisher.publishDeliveryStatusUpdated(delivery);
    }

    /**
     * 배송 추적
     */
    public DeliveryTrackingResponse trackDelivery(Long orderId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
            .orElseThrow(() -> new DeliveryNotFoundException());

        // 택배사 API를 통한 실시간 추적
        List<DeliveryTracking> trackings = trackerClient.track(
            delivery.getCarrier(),
            delivery.getTrackingNumber()
        );

        return DeliveryTrackingResponse.builder()
            .delivery(DeliveryResponse.from(delivery))
            .trackings(trackings)
            .build();
    }

    /**
     * 배송비 계산
     */
    public int calculateDeliveryFee(Order order) {
        // 판매자별, 배송 타입별 그룹핑
        Map<Seller, Map<DeliveryType, Integer>> grouped =
            groupBySellerAndDeliveryType(order);

        int totalFee = 0;

        for (Map.Entry<Seller, Map<DeliveryType, Integer>> sellerEntry : grouped.entrySet()) {
            for (Map.Entry<DeliveryType, Integer> typeEntry : sellerEntry.getValue().entrySet()) {
                DeliveryType type = typeEntry.getKey();
                int subtotal = typeEntry.getValue();

                // 로켓와우 회원은 무료
                if (order.getMember().getRocketWow().isActive()) {
                    continue;
                }

                // 무료배송 기준 확인
                if (subtotal >= type.getFreeThreshold()) {
                    continue;
                }

                totalFee += type.getBaseFee();
            }
        }

        return totalFee;
    }
}
```

---

## 🔍 Week 15-16: Search Service (Elasticsearch)

### Day 1-3: Elasticsearch 검색 구현

**ProductSearchService.java**:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final ProductSearchRepository searchRepository;

    /**
     * 상품 검색 (전문 검색)
     */
    public SearchResponse<ProductDocument> search(ProductSearchRequest request) {
        // 1. 검색 쿼리 생성
        NativeSearchQuery searchQuery = buildSearchQuery(request);

        // 2. 검색 실행
        SearchHits<ProductDocument> searchHits = elasticsearchOperations
            .search(searchQuery, ProductDocument.class);

        // 3. 결과 변환
        List<ProductSearchResult> results = searchHits.getSearchHits().stream()
            .map(hit -> ProductSearchResult.builder()
                .product(hit.getContent())
                .score(hit.getScore())
                .highlightedName(getHighlight(hit, "name"))
                .build())
            .collect(Collectors.toList());

        // 4. 집계 결과
        Map<String, Long> categoryAggregation = extractCategoryAggregation(searchHits);
        Map<String, Long> brandAggregation = extractBrandAggregation(searchHits);

        return SearchResponse.<ProductDocument>builder()
            .results(results)
            .totalHits(searchHits.getTotalHits())
            .aggregations(Map.of(
                "categories", categoryAggregation,
                "brands", brandAggregation
            ))
            .build();
    }

    /**
     * 자동완성
     */
    public List<String> autocomplete(String keyword) {
        NativeSearchQuery searchQuery = NativeSearchQueryBuilder.builder()
            .withQuery(
                QueryBuilders.multiMatchQuery(keyword)
                    .field("name.nori", 10.0f)        // 한글 형태소 분석
                    .field("name.ngram", 5.0f)        // n-gram
                    .field("name.jaso", 3.0f)         // 초성 검색
                    .type(MultiMatchQuery.Type.BEST_FIELDS)
                    .fuzziness(Fuzziness.AUTO)
            )
            .withPageable(PageRequest.of(0, 10))
            .build();

        SearchHits<ProductDocument> hits = elasticsearchOperations
            .search(searchQuery, ProductDocument.class);

        return hits.getSearchHits().stream()
            .map(hit -> hit.getContent().getName())
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * 검색 쿼리 빌더
     */
    private NativeSearchQuery buildSearchQuery(ProductSearchRequest request) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // 1. 키워드 검색
        if (StringUtils.hasText(request.getKeyword())) {
            boolQuery.must(
                QueryBuilders.multiMatchQuery(request.getKeyword())
                    .field("name", 10.0f)
                    .field("description", 5.0f)
                    .field("brand", 3.0f)
                    .type(MultiMatchQuery.Type.BEST_FIELDS)
                    .fuzziness(Fuzziness.AUTO)
            );
        }

        // 2. 카테고리 필터
        if (request.getCategoryId() != null) {
            boolQuery.filter(
                QueryBuilders.termQuery("categoryId", request.getCategoryId())
            );
        }

        // 3. 가격 범위 필터
        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            if (request.getMinPrice() != null) {
                rangeQuery.gte(request.getMinPrice());
            }
            if (request.getMaxPrice() != null) {
                rangeQuery.lte(request.getMaxPrice());
            }
            boolQuery.filter(rangeQuery);
        }

        // 4. 배송 타입 필터
        if (request.getDeliveryTypes() != null && !request.getDeliveryTypes().isEmpty()) {
            boolQuery.filter(
                QueryBuilders.termsQuery("deliveryType", request.getDeliveryTypes())
            );
        }

        // 5. 평점 필터
        if (request.getMinRating() != null) {
            boolQuery.filter(
                QueryBuilders.rangeQuery("averageRating").gte(request.getMinRating())
            );
        }

        // 6. 정렬
        List<SortBuilder<?>> sorts = buildSorts(request.getSort());

        // 7. 집계
        AggregationBuilder categoryAgg = AggregationBuilders
            .terms("categories")
            .field("categoryId")
            .size(20);

        AggregationBuilder brandAgg = AggregationBuilders
            .terms("brands")
            .field("brand.keyword")
            .size(20);

        // 8. 하이라이트
        HighlightBuilder highlightBuilder = new HighlightBuilder()
            .field("name")
            .field("description")
            .preTags("<em>")
            .postTags("</em>");

        return NativeSearchQueryBuilder.builder()
            .withQuery(boolQuery)
            .withSorts(sorts)
            .withAggregations(categoryAgg, brandAgg)
            .withHighlightBuilder(highlightBuilder)
            .withPageable(PageRequest.of(
                request.getPage(),
                request.getSize()
            ))
            .build();
    }

    private List<SortBuilder<?>> buildSorts(ProductSortType sort) {
        return switch (sort) {
            case LATEST -> List.of(
                SortBuilders.fieldSort("createdAt").order(SortOrder.DESC)
            );
            case PRICE_ASC -> List.of(
                SortBuilders.fieldSort("price").order(SortOrder.ASC)
            );
            case PRICE_DESC -> List.of(
                SortBuilders.fieldSort("price").order(SortOrder.DESC)
            );
            case POPULAR -> List.of(
                SortBuilders.fieldSort("salesCount").order(SortOrder.DESC),
                SortBuilders.scoreSort()
            );
            case RATING -> List.of(
                SortBuilders.fieldSort("averageRating").order(SortOrder.DESC),
                SortBuilders.fieldSort("reviewCount").order(SortOrder.DESC)
            );
            default -> List.of(SortBuilders.scoreSort());
        };
    }
}
```

**다음 파일**: plan-5-phase3-implementation.md에서 고급 기능(쿠폰, 리뷰, 추천 등) 구현을 다룹니다.
