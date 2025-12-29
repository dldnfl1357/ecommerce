# 🛒 이커머스 클론 프로젝트

> **Java + Spring Boot + JPA + MySQL** 기반의 복잡한 비즈니스 로직을 다루는 이커머스 플랫폼

---

## 📋 프로젝트 개요

### 목표
이커머스의 핵심 기능을 구현하여 **복잡한 비즈니스 로직**을 Java/Spring 생태계로 풀어내는 프로젝트

### 핵심 학습 포인트
- 복잡한 도메인 모델 설계 (DDD)
- JPA 연관관계 매핑 및 최적화
- 상태 패턴을 활용한 주문 상태 관리
- 동시성 제어 (재고 차감)
- 복잡한 가격/할인 계산 로직
- 트랜잭션 관리

### 기술 스택

| 분류 | 기술                         |
|------|----------------------------|
| **Language** | Java 17                    |
| **Framework** | Spring Boot 3.x            |
| **ORM** | Spring Data JPA, QueryDSL  |
| **Database** | MySQL 8.0                  |
| **Cache** | Redis (선택)                 |
| **Build** | Gradle                     |
| **Test** | JUnit 5, Mockito           |
| **Docs** | SpringRestDocs Asciidoctor |

---

## 🏗️ 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                        Client (Web/App)                      │
└─────────────────────────────┬───────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      API Gateway (Future)                    │
└─────────────────────────────┬───────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                   │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐           │
│  │ Member  │ │ Product │ │  Order  │ │ Payment │  ...      │
│  │ Module  │ │ Module  │ │ Module  │ │ Module  │           │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘           │
└─────────────────────────────┬───────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
        ┌──────────┐   ┌──────────┐   ┌──────────┐
        │  MySQL   │   │  Redis   │   │   S3     │
        │ (Master) │   │ (Cache)  │   │ (Image)  │
        └──────────┘   └──────────┘   └──────────┘
```

---

## 📦 도메인 분석

### 핵심 도메인 목록

| 도메인 | 설명 | 복잡도 | Phase |
|--------|------|--------|-------|
| **Member** | 회원 가입/로그인, 등급, 배송지 관리 | ⭐⭐⭐ | 1 |
| **Product** | 상품 등록, 옵션 조합, 카테고리 | ⭐⭐⭐⭐ | 1 |
| **Inventory** | 재고 관리, 동시성 제어 | ⭐⭐⭐⭐⭐ | 1 |
| **Cart** | 장바구니, 판매자별 묶음 | ⭐⭐⭐ | 1 |
| **Order** | 주문 생성, 상태 머신, 부분 취소 | ⭐⭐⭐⭐⭐ | 1 |
| **Payment** | 결제 처리, 복합 결제 | ⭐⭐⭐⭐ | 1 |
| **Delivery** | 배송 상태 관리, 배송비 계산 | ⭐⭐⭐⭐ | 1 |
| **Coupon** | 쿠폰 발급/사용, 중복 적용 규칙 | ⭐⭐⭐⭐⭐ | 2 |
| **Review** | 리뷰 작성, 평점 집계 | ⭐⭐⭐ | 2 |
| **Seller** | 판매자 관리, 정산 | ⭐⭐⭐⭐⭐ | 3 |
| **Refund** | 반품/환불 처리 | ⭐⭐⭐⭐ | 2 |

---

## 🎯 Phase 1: 핵심 구매 플로우

### 1.1 Member (회원)

#### 엔티티 설계

```java
@Entity
public class Member {
    @Id @GeneratedValue
    private Long id;
    
    private String email;
    private String password;
    private String name;
    private String phone;
    
    @Enumerated(EnumType.STRING)
    private MemberGrade grade;  // BRONZE, SILVER, GOLD, VIP
    
    private Integer point;  // 적립금
    
    @OneToMany(mappedBy = "member")
    private List<Address> addresses;  // 배송지 목록
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

@Entity
public class Address {
    @Id @GeneratedValue
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;
    
    private String name;        // 배송지명 (집, 회사 등)
    private String recipient;   // 수령인
    private String phone;
    private String zipCode;
    private String address;
    private String addressDetail;
    private Boolean isDefault;  // 기본 배송지 여부
}

public enum MemberGrade {
    BRONZE(0, 0.01),    // 1% 적립
    SILVER(100000, 0.02),  // 10만원 이상 구매, 2% 적립
    GOLD(500000, 0.03),    // 50만원 이상 구매, 3% 적립
    VIP(1000000, 0.05);    // 100만원 이상 구매, 5% 적립
    
    private final int threshold;
    private final double pointRate;
}
```

#### 비즈니스 규칙
- 이메일 중복 불가
- 기본 배송지는 회원당 1개만 가능
- 등급은 최근 6개월 구매 금액 기준 자동 갱신
- 적립금 사용 시 최소 1,000원 이상 보유 필요

---

### 1.2 Product (상품)

#### 엔티티 설계

```java
@Entity
public class Product {
    @Id @GeneratedValue
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private Seller seller;  // 판매자
    
    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;
    
    private String name;
    private String description;
    private Integer basePrice;  // 기본가
    
    @Enumerated(EnumType.STRING)
    private ProductStatus status;  // SALE, STOP, DELETED
    
    @Enumerated(EnumType.STRING)
    private DeliveryType deliveryType;  // ROCKET, ROCKET_FRESH, NORMAL
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<ProductOption> options;
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<ProductImage> images;
    
    private Double averageRating;  // 평균 평점
    private Integer reviewCount;
    private Integer salesCount;    // 판매량
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

@Entity
public class ProductOption {
    @Id @GeneratedValue
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;
    
    private String optionName;   // ex: "색상: 블랙 / 사이즈: L"
    private String option1;      // ex: "블랙"
    private String option2;      // ex: "L"
    private Integer addPrice;    // 추가 금액
    
    @OneToOne(mappedBy = "productOption")
    private Inventory inventory;
    
    private Boolean isAvailable;
}

@Entity
public class Category {
    @Id @GeneratedValue
    private Long id;
    
    private String name;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private Category parent;  // 상위 카테고리
    
    @OneToMany(mappedBy = "parent")
    private List<Category> children;  // 하위 카테고리
    
    private Integer depth;  // 0: 대분류, 1: 중분류, 2: 소분류
    private Integer sortOrder;
}

public enum DeliveryType {
    ROCKET("로켓배송", 0, 19800),       // 무료배송 기준 19,800원
    ROCKET_FRESH("로켓프레시", 0, 15000),
    NORMAL("일반배송", 3000, 30000);     // 기본 3,000원, 3만원 이상 무료
    
    private final String displayName;
    private final int baseFee;
    private final int freeThreshold;
}
```

#### 비즈니스 규칙
- 상품 옵션은 최대 2depth까지 (색상×사이즈)
- 옵션별로 재고와 추가금액이 다를 수 있음
- 카테고리는 3depth까지 (대분류 > 중분류 > 소분류)
- 상품 삭제는 소프트 삭제 (status = DELETED)

---

### 1.3 Inventory (재고)

#### 엔티티 설계

```java
@Entity
public class Inventory {
    @Id @GeneratedValue
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    private ProductOption productOption;
    
    private Integer quantity;      // 현재 재고
    private Integer reservedQuantity;  // 주문으로 예약된 수량
    
    @Version
    private Long version;  // 낙관적 락을 위한 버전
    
    private LocalDateTime updatedAt;
    
    // 가용 재고 = 현재 재고 - 예약 수량
    public Integer getAvailableQuantity() {
        return quantity - reservedQuantity;
    }
    
    // 재고 예약 (주문 시)
    public void reserve(int amount) {
        if (getAvailableQuantity() < amount) {
            throw new InsufficientStockException();
        }
        this.reservedQuantity += amount;
    }
    
    // 예약 확정 (결제 완료 시)
    public void confirm(int amount) {
        this.quantity -= amount;
        this.reservedQuantity -= amount;
    }
    
    // 예약 취소 (결제 실패/주문 취소 시)
    public void cancelReservation(int amount) {
        this.reservedQuantity -= amount;
    }
}
```

#### 비즈니스 규칙
- 재고 차감은 낙관적 락 또는 비관적 락 적용
- 주문 시 재고 예약 → 결제 완료 시 확정 → 결제 실패 시 예약 해제
- 재고 0 시 해당 옵션 품절 처리

#### 동시성 제어 전략

```java
// 방법 1: 비관적 락
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT i FROM Inventory i WHERE i.productOption.id = :optionId")
Optional<Inventory> findByOptionIdWithLock(@Param("optionId") Long optionId);

// 방법 2: 낙관적 락 + 재시도
@Retryable(value = OptimisticLockException.class, maxAttempts = 3)
public void decreaseStock(Long optionId, int quantity) {
    Inventory inventory = inventoryRepository.findByOptionId(optionId);
    inventory.reserve(quantity);
    // @Version으로 자동 충돌 감지
}
```

---

### 1.4 Cart (장바구니)

#### 엔티티 설계

```java
@Entity
public class Cart {
    @Id @GeneratedValue
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    private Member member;
    
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();
    
    private LocalDateTime updatedAt;
    
    // 판매자별로 그룹핑
    public Map<Seller, List<CartItem>> groupBySeller() {
        return items.stream()
            .collect(Collectors.groupingBy(
                item -> item.getProductOption().getProduct().getSeller()
            ));
    }
    
    // 총 금액 계산
    public int calculateTotalPrice() {
        return items.stream()
            .mapToInt(CartItem::calculatePrice)
            .sum();
    }
}

@Entity
public class CartItem {
    @Id @GeneratedValue
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private Cart cart;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private ProductOption productOption;
    
    private Integer quantity;
    private Boolean isSelected;  // 선택 여부
    
    private LocalDateTime addedAt;
    
    public int calculatePrice() {
        Product product = productOption.getProduct();
        return (product.getBasePrice() + productOption.getAddPrice()) * quantity;
    }
}
```

#### 비즈니스 규칙
- 동일 옵션 추가 시 수량 증가
- 품절 상품은 선택 불가 처리
- 장바구니 담긴 상품 가격 변경 시 알림

---

### 1.5 Order (주문) ⭐ 핵심

#### 엔티티 설계

```java
@Entity
@Table(name = "orders")
public class Order {
    @Id @GeneratedValue
    private Long id;
    
    private String orderNumber;  // 주문번호 (UUID 또는 날짜+시퀀스)
    
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();
    
    @Embedded
    private DeliveryInfo deliveryInfo;  // 배송 정보
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    // 금액 정보
    private Integer totalProductPrice;  // 상품 총액
    private Integer deliveryFee;        // 배송비
    private Integer discountAmount;     // 할인 금액
    private Integer couponDiscountAmount;  // 쿠폰 할인
    private Integer pointUsed;          // 사용 적립금
    private Integer finalPrice;         // 최종 결제 금액
    
    private LocalDateTime orderedAt;
    private LocalDateTime updatedAt;
}

@Entity
public class OrderItem {
    @Id @GeneratedValue
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private Order order;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private ProductOption productOption;
    
    // 주문 시점의 상품 정보 스냅샷 (상품 정보 변경되어도 주문 정보 유지)
    private String productName;
    private String optionName;
    private Integer price;          // 주문 시점 가격
    private Integer quantity;
    private Integer totalPrice;
    
    @Enumerated(EnumType.STRING)
    private OrderItemStatus status;
    
    private LocalDateTime updatedAt;
}

@Embeddable
public class DeliveryInfo {
    private String recipientName;
    private String recipientPhone;
    private String zipCode;
    private String address;
    private String addressDetail;
    private String deliveryMessage;
}

public enum OrderStatus {
    PENDING_PAYMENT,    // 결제 대기
    PAID,               // 결제 완료
    PREPARING,          // 상품 준비중
    SHIPPING,           // 배송중
    DELIVERED,          // 배송 완료
    CONFIRMED,          // 구매 확정
    CANCELLED,          // 주문 취소
    REFUND_REQUESTED,   // 환불 요청
    REFUNDED            // 환불 완료
}

public enum OrderItemStatus {
    ORDERED,            // 주문됨
    PREPARING,          // 준비중
    SHIPPING,           // 배송중
    DELIVERED,          // 배송완료
    CONFIRMED,          // 구매확정
    CANCEL_REQUESTED,   // 취소요청
    CANCELLED,          // 취소완료
    RETURN_REQUESTED,   // 반품요청
    RETURNED            // 반품완료
}
```

#### 상태 머신 (State Machine)

```
                    ┌───────────────────────────────────────────┐
                    │                                           │
                    ▼                                           │
┌─────────────┐   결제   ┌──────┐   준비완료  ┌───────────┐       │
│   PENDING   │ ──────▶ │ PAID │ ─────────▶ │ PREPARING │       │
│   PAYMENT   │         └──────┘            └───────────┘       │
└─────────────┘             │                    │              │
       │                    │                    │              │
       │ 취소                │ 취소               ▼              │
       │                    │               ┌──────────┐        │
       ▼                    ▼               │ SHIPPING │        │
┌─────────────┐       ┌───────────┐         └──────────┘        │
│  CANCELLED  │◀──────│ CANCELLED │             │               │
└─────────────┘       └───────────┘             │               │
                                                ▼               │
                                          ┌───────────┐         │
                      ┌───────────────────│ DELIVERED │         │
                      │                   └───────────┘         │
                      │                         │               │
                      ▼                         │ 구매확정       │
                ┌───────────┐                   ▼              │
                │  REFUND   │◀─반품───── ┌───────────┐          │
                │ REQUESTED │           │ CONFIRMED │──────────┘
                └───────────┘           └───────────┘
                      │
                      ▼
                ┌───────────┐
                │  REFUNDED │
                └───────────┘
```

#### 비즈니스 규칙

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    
    // 주문 생성
    @Transactional
    public Order createOrder(Long memberId, OrderCreateRequest request) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        
        // 1. 재고 확인 및 예약
        for (OrderItemRequest item : request.getItems()) {
            inventoryService.reserve(item.getOptionId(), item.getQuantity());
        }
        
        // 2. 주문 생성
        Order order = Order.create(member, request);
        
        // 3. 가격 계산
        order.calculatePrices();
        
        // 4. 쿠폰 적용
        if (request.getCouponId() != null) {
            couponService.use(request.getCouponId(), order);
        }
        
        // 5. 적립금 사용
        if (request.getPointToUse() > 0) {
            member.usePoint(request.getPointToUse());
            order.applyPoint(request.getPointToUse());
        }
        
        return orderRepository.save(order);
    }
    
    // 주문 취소
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        
        // 1. 취소 가능 상태 확인
        order.validateCancellable();
        
        // 2. 재고 복구
        for (OrderItem item : order.getOrderItems()) {
            inventoryService.cancelReservation(
                item.getProductOption().getId(), 
                item.getQuantity()
            );
        }
        
        // 3. 쿠폰 복구
        if (order.getCouponId() != null) {
            couponService.restore(order.getCouponId());
        }
        
        // 4. 적립금 복구
        if (order.getPointUsed() > 0) {
            order.getMember().restorePoint(order.getPointUsed());
        }
        
        // 5. 환불 처리 (결제 완료 상태였다면)
        if (order.getStatus() == OrderStatus.PAID) {
            paymentService.refund(order);
        }
        
        order.cancel();
    }
    
    // 부분 취소
    @Transactional
    public void cancelOrderItem(Long orderId, Long orderItemId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        OrderItem orderItem = order.findOrderItem(orderItemId);
        
        // 1. 취소 가능 확인
        orderItem.validateCancellable();
        
        // 2. 재고 복구
        inventoryService.cancelReservation(
            orderItem.getProductOption().getId(),
            orderItem.getQuantity()
        );
        
        // 3. 부분 환불 금액 계산
        int refundAmount = orderItem.getTotalPrice();
        
        // 4. 환불 처리
        paymentService.partialRefund(order, refundAmount);
        
        // 5. 상태 변경
        orderItem.cancel();
        
        // 6. 모든 아이템 취소되면 주문 전체 취소
        if (order.isAllItemsCancelled()) {
            order.cancel();
        }
    }
}
```

---

### 1.6 Payment (결제)

#### 엔티티 설계

```java
@Entity
public class Payment {
    @Id @GeneratedValue
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    private Order order;
    
    private String paymentKey;  // PG사 결제 키
    
    @Enumerated(EnumType.STRING)
    private PaymentMethod method;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    
    private Integer amount;
    private Integer refundedAmount;
    
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
}

public enum PaymentMethod {
    CARD,           // 신용/체크카드
    VIRTUAL_ACCOUNT,  // 가상계좌
    TRANSFER,       // 실시간 이체
    PHONE,          // 휴대폰 결제
    KAKAO_PAY,      // 카카오페이
    NAVER_PAY,      // 네이버페이
    TOSS_PAY        // 토스페이
}

public enum PaymentStatus {
    PENDING,        // 결제 대기
    COMPLETED,      // 결제 완료
    CANCELLED,      // 전체 취소
    PARTIAL_CANCELLED,  // 부분 취소
    FAILED          // 결제 실패
}
```

#### 결제 흐름

```java
@Service
@RequiredArgsConstructor
public class PaymentService {
    
    private final PaymentGateway paymentGateway;  // PG 연동 (Mock)
    
    // 결제 요청
    @Transactional
    public Payment requestPayment(Long orderId, PaymentRequest request) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        
        // 1. 주문 상태 확인
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidOrderStatusException();
        }
        
        // 2. PG사 결제 요청
        PaymentResult result = paymentGateway.pay(request);
        
        // 3. 결제 결과 처리
        if (result.isSuccess()) {
            Payment payment = Payment.create(order, request, result);
            paymentRepository.save(payment);
            
            // 4. 주문 상태 변경
            order.paid();
            
            // 5. 재고 확정
            for (OrderItem item : order.getOrderItems()) {
                inventoryService.confirm(
                    item.getProductOption().getId(),
                    item.getQuantity()
                );
            }
            
            // 6. 적립금 지급 (구매 확정 시로 이동 가능)
            // memberService.addPoint(order.getMember(), order.calculatePointEarned());
            
            return payment;
        } else {
            // 결제 실패 시 재고 예약 해제
            cancelStockReservation(order);
            throw new PaymentFailedException(result.getMessage());
        }
    }
    
    // 환불
    @Transactional
    public void refund(Order order) {
        Payment payment = paymentRepository.findByOrder(order).orElseThrow();
        
        // PG사 환불 요청
        paymentGateway.refund(payment.getPaymentKey(), payment.getAmount());
        
        payment.cancel();
    }
}
```

---

### 1.7 Delivery (배송)

#### 엔티티 설계

```java
@Entity
public class Delivery {
    @Id @GeneratedValue
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    private Order order;
    
    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;
    
    private String trackingNumber;  // 송장번호
    private String carrier;         // 택배사
    
    @Embedded
    private DeliveryInfo deliveryInfo;
    
    private Integer deliveryFee;
    
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    
    @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL)
    private List<DeliveryHistory> histories = new ArrayList<>();
}

@Entity
public class DeliveryHistory {
    @Id @GeneratedValue
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private Delivery delivery;
    
    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;
    
    private String location;   // 현재 위치
    private String description;
    
    private LocalDateTime createdAt;
}

public enum DeliveryStatus {
    PENDING,        // 배송 준비
    PICKED_UP,      // 집화
    IN_TRANSIT,     // 배송중
    OUT_FOR_DELIVERY,  // 배송 출발
    DELIVERED,      // 배송 완료
    CANCELLED       // 배송 취소
}
```

#### 배송비 계산 로직

```java
@Service
public class DeliveryFeeCalculator {
    
    public int calculate(Order order) {
        // 1. 판매자별 그룹핑
        Map<Seller, List<OrderItem>> sellerItems = order.groupBySeller();
        
        int totalFee = 0;
        
        for (Map.Entry<Seller, List<OrderItem>> entry : sellerItems.entrySet()) {
            Seller seller = entry.getKey();
            List<OrderItem> items = entry.getValue();
            
            // 2. 배송 타입별 그룹핑 (로켓배송/일반배송은 별도 배송비)
            Map<DeliveryType, List<OrderItem>> byDeliveryType = items.stream()
                .collect(Collectors.groupingBy(
                    item -> item.getProductOption().getProduct().getDeliveryType()
                ));
            
            for (Map.Entry<DeliveryType, List<OrderItem>> typeEntry : byDeliveryType.entrySet()) {
                DeliveryType type = typeEntry.getKey();
                int subtotal = typeEntry.getValue().stream()
                    .mapToInt(OrderItem::getTotalPrice)
                    .sum();
                
                // 3. 무료배송 기준 확인
                if (subtotal >= type.getFreeThreshold()) {
                    continue;  // 무료배송
                }
                
                totalFee += type.getBaseFee();
            }
        }
        
        return totalFee;
    }
}
```

---

## 🗄️ ERD (Entity Relationship Diagram)

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│    Member    │       │    Seller    │       │   Category   │
├──────────────┤       ├──────────────┤       ├──────────────┤
│ id           │       │ id           │       │ id           │
│ email        │       │ businessName │       │ name         │
│ password     │       │ email        │       │ parent_id    │──┐
│ name         │       │ phone        │       │ depth        │  │
│ phone        │       │ ...          │       │ sortOrder    │◀─┘
│ grade        │       └──────────────┘       └──────────────┘
│ point        │              │                      │
│ createdAt    │              │                      │
└──────────────┘              │                      │
       │                      │                      │
       │                      ▼                      ▼
       │               ┌──────────────┐       ┌──────────────┐
       │               │   Product    │──────▶│   Product    │
       │               ├──────────────┤       │    Image     │
       │               │ id           │       └──────────────┘
       │               │ seller_id    │
       │               │ category_id  │       ┌──────────────┐
       │               │ name         │──────▶│   Product    │
       │               │ basePrice    │       │    Option    │
       │               │ status       │       ├──────────────┤
       │               │ deliveryType │       │ id           │
       │               └──────────────┘       │ product_id   │
       │                                      │ optionName   │
       │                                      │ addPrice     │
       │                                      └──────────────┘
       │                                             │
       │                                             │ 1:1
       │                                             ▼
       │       ┌──────────────┐              ┌──────────────┐
       │       │     Cart     │              │  Inventory   │
       │       ├──────────────┤              ├──────────────┤
       └──────▶│ id           │              │ id           │
               │ member_id    │              │ option_id    │
               └──────────────┘              │ quantity     │
                      │                      │ reserved     │
                      │ 1:N                  │ version      │
                      ▼                      └──────────────┘
               ┌──────────────┐
               │   CartItem   │
               ├──────────────┤
               │ id           │
               │ cart_id      │
               │ option_id    │
               │ quantity     │
               │ isSelected   │
               └──────────────┘


       ┌──────────────┐
       │    Order     │
       ├──────────────┤
       │ id           │
       │ orderNumber  │───────────────────────┐
       │ member_id    │                       │
       │ status       │                       │ 1:1
       │ totalPrice   │                       ▼
       │ deliveryFee  │              ┌──────────────┐
       │ finalPrice   │              │   Payment    │
       │ orderedAt    │              ├──────────────┤
       └──────────────┘              │ id           │
              │                      │ order_id     │
              │ 1:N                  │ paymentKey   │
              ▼                      │ method       │
       ┌──────────────┐              │ status       │
       │  OrderItem   │              │ amount       │
       ├──────────────┤              └──────────────┘
       │ id           │
       │ order_id     │
       │ option_id    │              ┌──────────────┐
       │ productName  │◀─────────────│   Delivery   │
       │ optionName   │   1:1        ├──────────────┤
       │ price        │              │ id           │
       │ quantity     │              │ order_id     │
       │ status       │              │ status       │
       └──────────────┘              │ trackingNo   │
                                     │ carrier      │
                                     └──────────────┘
```

---

## 📡 API 설계

### Member API

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/v1/members/signup` | 회원가입 |
| POST | `/api/v1/members/login` | 로그인 |
| GET | `/api/v1/members/me` | 내 정보 조회 |
| PUT | `/api/v1/members/me` | 내 정보 수정 |
| GET | `/api/v1/members/me/addresses` | 배송지 목록 |
| POST | `/api/v1/members/me/addresses` | 배송지 추가 |
| PUT | `/api/v1/members/me/addresses/{id}` | 배송지 수정 |
| DELETE | `/api/v1/members/me/addresses/{id}` | 배송지 삭제 |

### Product API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/v1/products` | 상품 목록 (페이징, 필터링) |
| GET | `/api/v1/products/{id}` | 상품 상세 |
| GET | `/api/v1/products/{id}/options` | 상품 옵션 목록 |
| GET | `/api/v1/categories` | 카테고리 목록 |
| GET | `/api/v1/categories/{id}/products` | 카테고리별 상품 |

### Cart API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/v1/cart` | 장바구니 조회 |
| POST | `/api/v1/cart/items` | 장바구니 추가 |
| PUT | `/api/v1/cart/items/{id}` | 수량 변경 |
| DELETE | `/api/v1/cart/items/{id}` | 장바구니 삭제 |
| PUT | `/api/v1/cart/items/{id}/select` | 선택/해제 |

### Order API

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/v1/orders` | 주문 생성 |
| GET | `/api/v1/orders` | 주문 목록 |
| GET | `/api/v1/orders/{id}` | 주문 상세 |
| POST | `/api/v1/orders/{id}/cancel` | 주문 취소 |
| POST | `/api/v1/orders/{id}/items/{itemId}/cancel` | 부분 취소 |
| POST | `/api/v1/orders/{id}/confirm` | 구매 확정 |

### Payment API

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/v1/payments` | 결제 요청 |
| POST | `/api/v1/payments/{id}/confirm` | 결제 승인 (PG 콜백) |
| GET | `/api/v1/payments/{id}` | 결제 정보 조회 |

---

## 🗓️ 개발 로드맵

### Phase 1: 핵심 구매 플로우 (4~5주)

| 주차 | 작업 내용 |
|------|----------|
| **1주차** | 프로젝트 셋업, Member/Address 도메인, 인증 (JWT) |
| **2주차** | Product/Category/Option 도메인, 상품 목록/상세 API |
| **3주차** | Inventory 도메인 (동시성 제어), Cart 도메인 |
| **4주차** | Order 도메인 (상태 머신), 주문 생성/취소 |
| **5주차** | Payment/Delivery 도메인, 통합 테스트 |

### Phase 2: 확장 기능 (3주)

| 주차 | 작업 내용 |
|------|----------|
| **6주차** | Coupon 도메인 (발급/사용/중복 규칙) |
| **7주차** | Review 도메인, 반품/환불 처리 |
| **8주차** | 회원 등급/적립금, 검색 기능 |

### Phase 3: 심화 (선택)

- 판매자 시스템 & 정산
- Elasticsearch 검색
- 알림 시스템 (이메일/푸시)
- 성능 최적화 (캐싱, 쿼리 튜닝)

---

## 🧪 테스트 전략

### 테스트 레이어

```
┌─────────────────────────────────────────┐
│         E2E Test (Acceptance)           │  ← 주요 시나리오
├─────────────────────────────────────────┤
│         Integration Test                 │  ← Repository, 외부 연동
├─────────────────────────────────────────┤
│           Unit Test                      │  ← Domain, Service
└─────────────────────────────────────────┘
```

### 주요 테스트 시나리오

```java
// 1. 재고 동시성 테스트
@Test
void 동시에_100명이_같은_상품_주문시_재고가_정확히_차감된다() {
    // given
    Inventory inventory = createInventory(quantity = 50);
    
    // when
    ExecutorService executor = Executors.newFixedThreadPool(100);
    CountDownLatch latch = new CountDownLatch(100);
    
    for (int i = 0; i < 100; i++) {
        executor.submit(() -> {
            try {
                orderService.createOrder(request);
            } finally {
                latch.countDown();
            }
        });
    }
    latch.await();
    
    // then
    assertThat(inventory.getQuantity()).isEqualTo(0);
    assertThat(orderRepository.count()).isEqualTo(50);  // 50개만 성공
}

// 2. 주문 상태 전이 테스트
@Test
void 배송중_상태에서는_취소할_수_없다() {
    // given
    Order order = createOrder(status = SHIPPING);
    
    // when & then
    assertThatThrownBy(() -> orderService.cancel(order.getId()))
        .isInstanceOf(InvalidOrderStatusException.class);
}

// 3. 쿠폰 중복 적용 테스트
@Test
void 동일_카테고리_쿠폰은_중복_적용할_수_없다() {
    // given
    Coupon coupon1 = createCategoryCoupon(categoryId = 1);
    Coupon coupon2 = createCategoryCoupon(categoryId = 1);
    
    // when & then
    assertThatThrownBy(() -> 
        orderService.applyCoupons(order, List.of(coupon1, coupon2)))
        .isInstanceOf(DuplicateCouponException.class);
}
```

---

## 📁 프로젝트 구조

```
src/main/java/com/example/ecommerce/
├── EcommerceApplication.java
│
├── domain/
│   ├── member/
│   │   ├── entity/
│   │   │   ├── Member.java
│   │   │   └── Address.java
│   │   ├── repository/
│   │   │   └── MemberRepository.java
│   │   ├── service/
│   │   │   └── MemberService.java
│   │   └── dto/
│   │       ├── MemberRequest.java
│   │       └── MemberResponse.java
│   │
│   ├── product/
│   │   ├── entity/
│   │   ├── repository/
│   │   ├── service/
│   │   └── dto/
│   │
│   ├── inventory/
│   │   ├── entity/
│   │   ├── repository/
│   │   └── service/
│   │
│   ├── cart/
│   │   └── ...
│   │
│   ├── order/
│   │   ├── entity/
│   │   │   ├── Order.java
│   │   │   ├── OrderItem.java
│   │   │   ├── OrderStatus.java
│   │   │   └── OrderItemStatus.java
│   │   ├── repository/
│   │   ├── service/
│   │   │   ├── OrderService.java
│   │   │   └── OrderValidator.java
│   │   └── dto/
│   │
│   ├── payment/
│   │   └── ...
│   │
│   └── delivery/
│       └── ...
│
├── api/
│   ├── member/
│   │   └── MemberController.java
│   ├── product/
│   │   └── ProductController.java
│   ├── cart/
│   │   └── CartController.java
│   ├── order/
│   │   └── OrderController.java
│   └── payment/
│       └── PaymentController.java
│
├── global/
│   ├── config/
│   │   ├── JpaConfig.java
│   │   ├── SecurityConfig.java
│   │   └── SwaggerConfig.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── BusinessException.java
│   │   └── ErrorCode.java
│   ├── auth/
│   │   ├── JwtTokenProvider.java
│   │   └── AuthInterceptor.java
│   └── common/
│       ├── BaseEntity.java
│       └── ApiResponse.java
│
└── infra/
    └── payment/
        ├── PaymentGateway.java
        └── MockPaymentGateway.java
```

---

## ⚙️ 설정 파일

### application.yml

```yaml
spring:
  profiles:
    active: local

  datasource:
    url: jdbc:mysql://localhost:3306/ecommerce?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
        default_batch_fetch_size: 100
    open-in-view: false

  flyway:
    enabled: true
    baseline-on-migrate: true

logging:
  level:
    org.hibernate.SQL: debug
    org.hibernate.type.descriptor.sql: trace

jwt:
  secret: ${JWT_SECRET}
  expiration: 3600000  # 1시간
```

### build.gradle

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.example'
version = '0.0.1-SNAPSHOT'

java {
    sourceCompatibility = '17'
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    
    // Database
    runtimeOnly 'com.mysql:mysql-connector-j'
    implementation 'org.flywaydb:flyway-core'
    implementation 'org.flywaydb:flyway-mysql'
    
    // QueryDSL
    implementation 'com.querydsl:querydsl-jpa:5.0.0:jakarta'
    annotationProcessor 'com.querydsl:querydsl-apt:5.0.0:jakarta'
    annotationProcessor 'jakarta.annotation:jakarta.annotation-api'
    annotationProcessor 'jakarta.persistence:jakarta.persistence-api'
    
    // JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.12.3'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.3'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.3'
    
    // Swagger
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
    
    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    
    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testRuntimeOnly 'com.h2database:h2'
}

test {
    useJUnitPlatform()
}
```

---

## 🚀 실행 방법

### 1. MySQL 설치 및 데이터베이스 생성

```sql
CREATE DATABASE ecommerce CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 환경변수 설정

```bash
export JWT_SECRET=your-secret-key-here
```

### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 4. API 문서 확인

```
http://localhost:8080/swagger-ui.html
```

---

## 📝 참고 자료

- [쿠팡 기술 블로그](https://medium.com/coupang-engineering)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [QueryDSL Reference](http://querydsl.com/static/querydsl/latest/reference/html/)

---

**Made with ❤️ for E-commerce Systems**
