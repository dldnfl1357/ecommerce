# 쿠팡 클론 프로젝트 기획서 (2/6) - 도메인 모델 상세 설계

> **DDD(Domain-Driven Design) 기반 도메인 모델링**

---

## 📋 목차
1. [도메인 개요](#도메인-개요)
2. [Member 도메인](#member-도메인)
3. [Product 도메인](#product-도메인)
4. [Inventory 도메인](#inventory-도메인)
5. [Cart 도메인](#cart-도메인)
6. [Order 도메인](#order-도메인)
7. [Payment 도메인](#payment-도메인)
8. [Delivery 도메인](#delivery-도메인)
9. [Coupon 도메인](#coupon-도메인)
10. [Review 도메인](#review-도메인)
11. [Seller 도메인](#seller-도메인)
12. [도메인 이벤트](#도메인-이벤트)

---

## 🎯 도메인 개요

### 도메인 분류

#### Core Domain (핵심 도메인)
- **Order**: 주문 처리 (가장 복잡하고 중요)
- **Payment**: 결제 처리
- **Inventory**: 재고 관리

#### Supporting Domain (지원 도메인)
- **Product**: 상품 관리
- **Member**: 회원 관리
- **Delivery**: 배송 관리

#### Generic Domain (일반 도메인)
- **Coupon**: 쿠폰 관리
- **Review**: 리뷰 관리
- **Notification**: 알림

---

## 👤 Member 도메인

### 1. 엔티티 설계

#### 1.1 Member (회원)

```java
@Entity
@Table(name = "members", indexes = {
    @Index(name = "idx_email", columnList = "email", unique = true),
    @Index(name = "idx_phone", columnList = "phone")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;  // BCrypt 암호화

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberGrade grade;

    @Column(nullable = false)
    private Integer point = 0;  // 적립금

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;  // ACTIVE, DORMANT, WITHDRAWN

    @Column
    private LocalDateTime lastLoginAt;

    @Column
    private LocalDateTime withdrawnAt;

    // 로켓와우 회원 여부
    @Embedded
    private RocketWowMembership rocketWow;

    // 연관관계
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<Address> addresses = new ArrayList<>();

    // 비즈니스 로직
    public void login() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void usePoint(int amount) {
        if (this.point < amount) {
            throw new InsufficientPointException();
        }
        this.point -= amount;
    }

    public void earnPoint(int amount) {
        this.point += amount;
    }

    public void upgradeGrade(MemberGrade newGrade) {
        if (newGrade.getLevel() <= this.grade.getLevel()) {
            throw new InvalidGradeUpgradeException();
        }
        this.grade = newGrade;
    }

    public void withdraw() {
        this.status = MemberStatus.WITHDRAWN;
        this.withdrawnAt = LocalDateTime.now();
    }
}
```

#### 1.2 Address (배송지)

```java
@Entity
@Table(name = "addresses", indexes = {
    @Index(name = "idx_member_id", columnList = "member_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Address extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 50)
    private String name;  // 배송지명: "집", "회사" 등

    @Column(nullable = false, length = 50)
    private String recipient;  // 수령인

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 10)
    private String zipCode;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(length = 255)
    private String addressDetail;

    @Column(nullable = false)
    private Boolean isDefault = false;

    @Column(length = 100)
    private String deliveryRequest;  // 배송 요청사항

    // 비즈니스 로직
    public void setAsDefault() {
        this.isDefault = true;
    }

    public void unsetDefault() {
        this.isDefault = false;
    }
}
```

#### 1.3 RocketWowMembership (로켓와우 구독)

```java
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class RocketWowMembership {

    @Column(name = "rocket_wow_active")
    private Boolean active = false;

    @Column(name = "rocket_wow_started_at")
    private LocalDateTime startedAt;

    @Column(name = "rocket_wow_expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "rocket_wow_auto_renewal")
    private Boolean autoRenewal = false;

    public static RocketWowMembership subscribe() {
        RocketWowMembership membership = new RocketWowMembership();
        membership.active = true;
        membership.startedAt = LocalDateTime.now();
        membership.expiresAt = LocalDateTime.now().plusMonths(1);
        return membership;
    }

    public boolean isActive() {
        return active && expiresAt.isAfter(LocalDateTime.now());
    }

    public void cancel() {
        this.active = false;
        this.autoRenewal = false;
    }
}
```

### 2. Value Objects

```java
public enum MemberGrade {
    BRONZE(1, 0, 0.01),
    SILVER(2, 100_000, 0.02),
    GOLD(3, 500_000, 0.03),
    VIP(4, 1_000_000, 0.05);

    private final int level;
    private final int threshold;  // 최근 6개월 구매 금액 기준
    private final double pointRate;  // 적립률

    // 구매 금액 기준으로 등급 계산
    public static MemberGrade calculateGrade(int totalPurchaseAmount) {
        if (totalPurchaseAmount >= VIP.threshold) return VIP;
        if (totalPurchaseAmount >= GOLD.threshold) return GOLD;
        if (totalPurchaseAmount >= SILVER.threshold) return SILVER;
        return BRONZE;
    }
}

public enum MemberStatus {
    ACTIVE,      // 활성
    DORMANT,     // 휴면 (1년 미접속)
    WITHDRAWN    // 탈퇴
}
```

### 3. Repository

```java
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    Optional<Member> findByPhone(String phone);

    @Query("SELECT m FROM Member m WHERE m.status = :status " +
           "AND m.lastLoginAt < :dormantDate")
    List<Member> findDormantMembers(
        @Param("status") MemberStatus status,
        @Param("dormantDate") LocalDateTime dormantDate
    );

    @Query("SELECT SUM(o.finalPrice) FROM Order o " +
           "WHERE o.member.id = :memberId " +
           "AND o.status = 'CONFIRMED' " +
           "AND o.orderedAt >= :since")
    Integer calculateTotalPurchaseAmount(
        @Param("memberId") Long memberId,
        @Param("since") LocalDateTime since
    );
}
```

---

## 📦 Product 도메인

### 1. 엔티티 설계

#### 1.1 Product (상품)

```java
@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_seller_id", columnList = "seller_id"),
    @Index(name = "idx_category_id", columnList = "category_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer basePrice;  // 기본 가격

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryType deliveryType;

    @Column(nullable = false)
    private Integer deliveryFee;

    @Column(nullable = false)
    private Integer freeDeliveryThreshold;  // 무료배송 기준

    // 통계 정보
    @Column(nullable = false)
    private Double averageRating = 0.0;

    @Column(nullable = false)
    private Integer reviewCount = 0;

    @Column(nullable = false)
    private Integer salesCount = 0;

    @Column(nullable = false)
    private Integer viewCount = 0;

    // 연관관계
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductOption> options = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    // 비즈니스 로직
    public void activate() {
        if (!hasStock()) {
            throw new NoStockException();
        }
        this.status = ProductStatus.ON_SALE;
    }

    public void deactivate() {
        this.status = ProductStatus.STOP_SALE;
    }

    public void delete() {
        this.status = ProductStatus.DELETED;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void updateRating(double newRating) {
        this.averageRating = ((this.averageRating * this.reviewCount) + newRating)
                           / (this.reviewCount + 1);
        this.reviewCount++;
    }

    public void increaseSalesCount(int quantity) {
        this.salesCount += quantity;
    }

    private boolean hasStock() {
        return options.stream()
                     .anyMatch(option -> option.getInventory().getAvailableQuantity() > 0);
    }
}
```

#### 1.2 ProductOption (상품 옵션)

```java
@Entity
@Table(name = "product_options", indexes = {
    @Index(name = "idx_product_id", columnList = "product_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ProductOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 100)
    private String optionName;  // "색상: 블랙 / 사이즈: L"

    @Column(length = 50)
    private String option1;  // "블랙"

    @Column(length = 50)
    private String option2;  // "L"

    @Column(nullable = false)
    private Integer addPrice;  // 추가 금액

    @Column(nullable = false)
    private Boolean isAvailable = true;

    @OneToOne(mappedBy = "productOption", cascade = CascadeType.ALL)
    private Inventory inventory;

    // 비즈니스 로직
    public int calculatePrice() {
        return product.getBasePrice() + addPrice;
    }

    public void disable() {
        this.isAvailable = false;
    }

    public void enable() {
        this.isAvailable = true;
    }
}
```

#### 1.3 Category (카테고리)

```java
@Entity
@Table(name = "categories", indexes = {
    @Index(name = "idx_parent_id", columnList = "parent_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Category> children = new ArrayList<>();

    @Column(nullable = false)
    private Integer depth;  // 0: 대분류, 1: 중분류, 2: 소분류

    @Column(nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private Boolean isActive = true;

    // 비즈니스 로직
    public boolean isLeaf() {
        return children.isEmpty();
    }

    public List<Category> getAncestors() {
        List<Category> ancestors = new ArrayList<>();
        Category current = this.parent;
        while (current != null) {
            ancestors.add(0, current);
            current = current.getParent();
        }
        return ancestors;
    }
}
```

#### 1.4 ProductImage (상품 이미지)

```java
@Entity
@Table(name = "product_images", indexes = {
    @Index(name = "idx_product_id", columnList = "product_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ProductImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 500)
    private String imageUrl;  // S3 URL

    @Column(nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private Boolean isThumbnail = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImageType type;  // MAIN, DETAIL, REVIEW
}
```

### 2. Value Objects

```java
public enum ProductStatus {
    ON_SALE,      // 판매중
    STOP_SALE,    // 판매중지
    SOLD_OUT,     // 품절
    DELETED       // 삭제됨
}

public enum DeliveryType {
    ROCKET("로켓배송", 0, 19_800),
    ROCKET_FRESH("로켓프레시", 0, 15_000),
    DAWN("새벽배송", 0, 30_000),
    NORMAL("일반배송", 3_000, 30_000);

    private final String displayName;
    private final int baseFee;
    private final int freeThreshold;

    public int calculateDeliveryFee(int orderAmount) {
        if (orderAmount >= freeThreshold) {
            return 0;
        }
        return baseFee;
    }
}

public enum ImageType {
    MAIN,      // 메인 이미지
    DETAIL,    // 상세 이미지
    REVIEW     // 리뷰 이미지
}
```

---

## 📊 Inventory 도메인

### 1. 엔티티 설계

```java
@Entity
@Table(name = "inventories", indexes = {
    @Index(name = "idx_product_option_id", columnList = "product_option_id", unique = true)
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Inventory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_option_id", nullable = false, unique = true)
    private ProductOption productOption;

    @Column(nullable = false)
    private Integer quantity;  // 실제 재고

    @Column(nullable = false)
    private Integer reservedQuantity = 0;  // 예약된 재고

    @Version
    private Long version;  // 낙관적 락

    // 비즈니스 로직
    public Integer getAvailableQuantity() {
        return quantity - reservedQuantity;
    }

    /**
     * 재고 예약 (주문 시)
     */
    public void reserve(int amount) {
        if (getAvailableQuantity() < amount) {
            throw new InsufficientStockException(
                "재고 부족: 요청=" + amount + ", 가용=" + getAvailableQuantity()
            );
        }
        this.reservedQuantity += amount;
    }

    /**
     * 예약 확정 (결제 완료 시)
     */
    public void confirmReservation(int amount) {
        if (this.reservedQuantity < amount) {
            throw new IllegalStateException("예약된 수량보다 확정 수량이 많습니다");
        }
        this.quantity -= amount;
        this.reservedQuantity -= amount;
    }

    /**
     * 예약 취소 (주문 취소/결제 실패 시)
     */
    public void cancelReservation(int amount) {
        if (this.reservedQuantity < amount) {
            throw new IllegalStateException("예약된 수량보다 취소 수량이 많습니다");
        }
        this.reservedQuantity -= amount;
    }

    /**
     * 재고 추가
     */
    public void increase(int amount) {
        this.quantity += amount;
    }

    /**
     * 재고 차감 (직접 차감, 주의!)
     */
    public void decrease(int amount) {
        if (this.quantity < amount) {
            throw new InsufficientStockException();
        }
        this.quantity -= amount;
    }
}
```

### 2. 동시성 제어 전략

```java
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductOptionId(Long productOptionId);

    // 비관적 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productOption.id = :optionId")
    Optional<Inventory> findByProductOptionIdWithLock(@Param("optionId") Long optionId);

    // 낙관적 락은 @Version으로 자동 처리
}

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    /**
     * 낙관적 락 + 재시도 전략
     */
    @Transactional
    @Retryable(
        value = OptimisticLockException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    public void reserveWithOptimisticLock(Long optionId, int quantity) {
        Inventory inventory = inventoryRepository
            .findByProductOptionId(optionId)
            .orElseThrow();

        inventory.reserve(quantity);
        // @Version으로 자동 충돌 감지
    }

    /**
     * 비관적 락 전략 (확실하지만 느림)
     */
    @Transactional
    public void reserveWithPessimisticLock(Long optionId, int quantity) {
        Inventory inventory = inventoryRepository
            .findByProductOptionIdWithLock(optionId)
            .orElseThrow();

        inventory.reserve(quantity);
    }

    /**
     * Redis 분산 락 (권장)
     */
    @Transactional
    public void reserveWithDistributedLock(Long optionId, int quantity) {
        String lockKey = "inventory:lock:" + optionId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(10, 5, TimeUnit.SECONDS);
            if (!acquired) {
                throw new LockAcquisitionException();
            }

            Inventory inventory = inventoryRepository
                .findByProductOptionId(optionId)
                .orElseThrow();

            inventory.reserve(quantity);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

---

## 🛒 Cart 도메인

### 1. 엔티티 설계

```java
@Entity
@Table(name = "carts", indexes = {
    @Index(name = "idx_member_id", columnList = "member_id", unique = true)
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Cart extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    // 비즈니스 로직
    public void addItem(ProductOption option, int quantity) {
        CartItem existingItem = findItem(option.getId());

        if (existingItem != null) {
            // 이미 있으면 수량 증가
            existingItem.increaseQuantity(quantity);
        } else {
            // 없으면 새로 추가
            CartItem newItem = CartItem.create(this, option, quantity);
            items.add(newItem);
        }
    }

    public void removeItem(Long itemId) {
        items.removeIf(item -> item.getId().equals(itemId));
    }

    public void clearItems() {
        items.clear();
    }

    public Map<Seller, List<CartItem>> groupBySeller() {
        return items.stream()
            .filter(CartItem::isSelected)
            .collect(Collectors.groupingBy(
                item -> item.getProductOption().getProduct().getSeller()
            ));
    }

    public int calculateTotalPrice() {
        return items.stream()
            .filter(CartItem::isSelected)
            .mapToInt(CartItem::calculatePrice)
            .sum();
    }

    private CartItem findItem(Long optionId) {
        return items.stream()
            .filter(item -> item.getProductOption().getId().equals(optionId))
            .findFirst()
            .orElse(null);
    }
}

@Entity
@Table(name = "cart_items", indexes = {
    @Index(name = "idx_cart_id", columnList = "cart_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class CartItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_option_id", nullable = false)
    private ProductOption productOption;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Boolean isSelected = true;

    public static CartItem create(Cart cart, ProductOption option, int quantity) {
        CartItem item = new CartItem();
        item.cart = cart;
        item.productOption = option;
        item.quantity = quantity;
        return item;
    }

    public void increaseQuantity(int amount) {
        this.quantity += amount;
    }

    public void updateQuantity(int newQuantity) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("수량은 1개 이상이어야 합니다");
        }
        this.quantity = newQuantity;
    }

    public void toggleSelection() {
        this.isSelected = !this.isSelected;
    }

    public int calculatePrice() {
        return productOption.calculatePrice() * quantity;
    }
}
```

---

## 📝 Order 도메인 (핵심)

### 1. 엔티티 설계

```java
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_number", columnList = "order_number", unique = true),
    @Index(name = "idx_member_id", columnList = "member_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_ordered_at", columnList = "ordered_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String orderNumber;  // ORD-20240101-000001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Embedded
    private DeliveryInfo deliveryInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    // 금액 정보
    @Column(nullable = false)
    private Integer totalProductPrice;  // 상품 총액

    @Column(nullable = false)
    private Integer totalDeliveryFee;  // 배송비 총액

    @Column(nullable = false)
    private Integer discountAmount = 0;  // 할인 금액

    @Column(nullable = false)
    private Integer couponDiscountAmount = 0;  // 쿠폰 할인

    @Column(nullable = false)
    private Integer pointUsed = 0;  // 사용 적립금

    @Column(nullable = false)
    private Integer finalPrice;  // 최종 결제 금액

    @Column(nullable = false)
    private LocalDateTime orderedAt;

    // 주문 생성
    public static Order create(
        Member member,
        List<OrderItemCreateRequest> itemRequests,
        DeliveryInfo deliveryInfo
    ) {
        Order order = new Order();
        order.orderNumber = generateOrderNumber();
        order.member = member;
        order.deliveryInfo = deliveryInfo;
        order.status = OrderStatus.PENDING_PAYMENT;
        order.orderedAt = LocalDateTime.now();

        // 주문 항목 생성
        for (OrderItemCreateRequest request : itemRequests) {
            OrderItem item = OrderItem.create(
                order,
                request.getProductOption(),
                request.getQuantity()
            );
            order.orderItems.add(item);
        }

        // 가격 계산
        order.calculatePrices();

        return order;
    }

    // 가격 계산
    public void calculatePrices() {
        // 1. 상품 총액
        this.totalProductPrice = orderItems.stream()
            .mapToInt(OrderItem::getTotalPrice)
            .sum();

        // 2. 배송비 계산 (판매자별, 배송 타입별 그룹핑)
        this.totalDeliveryFee = calculateDeliveryFee();

        // 3. 최종 금액
        this.finalPrice = totalProductPrice
                        + totalDeliveryFee
                        - discountAmount
                        - couponDiscountAmount
                        - pointUsed;
    }

    // 쿠폰 적용
    public void applyCoupon(int couponDiscount) {
        this.couponDiscountAmount = couponDiscount;
        calculatePrices();
    }

    // 적립금 사용
    public void usePoint(int point) {
        this.pointUsed = point;
        calculatePrices();
    }

    // 상태 전이
    public void paid() {
        validateStatusTransition(OrderStatus.PAID);
        this.status = OrderStatus.PAID;
    }

    public void preparing() {
        validateStatusTransition(OrderStatus.PREPARING);
        this.status = OrderStatus.PREPARING;
    }

    public void shipping() {
        validateStatusTransition(OrderStatus.SHIPPING);
        this.status = OrderStatus.SHIPPING;
    }

    public void delivered() {
        validateStatusTransition(OrderStatus.DELIVERED);
        this.status = OrderStatus.DELIVERED;
    }

    public void confirm() {
        validateStatusTransition(OrderStatus.CONFIRMED);
        this.status = OrderStatus.CONFIRMED;
    }

    public void cancel() {
        if (!isCancellable()) {
            throw new OrderNotCancellableException();
        }
        this.status = OrderStatus.CANCELLED;
    }

    // 취소 가능 여부
    public boolean isCancellable() {
        return status == OrderStatus.PENDING_PAYMENT
            || status == OrderStatus.PAID
            || status == OrderStatus.PREPARING;
    }

    // 배송비 계산 로직
    private int calculateDeliveryFee() {
        Map<Seller, Map<DeliveryType, Integer>> sellerDeliveryMap =
            orderItems.stream()
                .collect(Collectors.groupingBy(
                    item -> item.getProductOption().getProduct().getSeller(),
                    Collectors.groupingBy(
                        item -> item.getProductOption().getProduct().getDeliveryType(),
                        Collectors.summingInt(OrderItem::getTotalPrice)
                    )
                ));

        int totalFee = 0;
        for (Map<DeliveryType, Integer> deliveryMap : sellerDeliveryMap.values()) {
            for (Map.Entry<DeliveryType, Integer> entry : deliveryMap.entrySet()) {
                DeliveryType type = entry.getKey();
                int subtotal = entry.getValue();
                totalFee += type.calculateDeliveryFee(subtotal);
            }
        }

        return totalFee;
    }

    // 상태 전이 검증
    private void validateStatusTransition(OrderStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new InvalidOrderStatusTransitionException(status, newStatus);
        }
    }

    private static String generateOrderNumber() {
        LocalDateTime now = LocalDateTime.now();
        String dateStr = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = UUID.randomUUID().toString().substring(0, 8);
        return "ORD-" + dateStr + "-" + randomStr.toUpperCase();
    }
}
```

### 2. OrderItem

```java
@Entity
@Table(name = "order_items", indexes = {
    @Index(name = "idx_order_id", columnList = "order_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_option_id", nullable = false)
    private ProductOption productOption;

    // 주문 시점 스냅샷 (상품 정보 변경되어도 유지)
    @Column(nullable = false, length = 200)
    private String productName;

    @Column(nullable = false, length = 100)
    private String optionName;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderItemStatus status;

    public static OrderItem create(Order order, ProductOption option, int quantity) {
        OrderItem item = new OrderItem();
        item.order = order;
        item.productOption = option;
        item.productName = option.getProduct().getName();
        item.optionName = option.getOptionName();
        item.price = option.calculatePrice();
        item.quantity = quantity;
        item.totalPrice = item.price * quantity;
        item.status = OrderItemStatus.ORDERED;
        return item;
    }

    public void cancel() {
        if (!isCancellable()) {
            throw new OrderItemNotCancellableException();
        }
        this.status = OrderItemStatus.CANCELLED;
    }

    public boolean isCancellable() {
        return status == OrderItemStatus.ORDERED
            || status == OrderItemStatus.PREPARING;
    }
}
```

### 3. Value Objects & Enums

```java
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@AllArgsConstructor
public class DeliveryInfo {

    @Column(nullable = false, length = 50)
    private String recipientName;

    @Column(nullable = false, length = 20)
    private String recipientPhone;

    @Column(nullable = false, length = 10)
    private String zipCode;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(length = 255)
    private String addressDetail;

    @Column(length = 200)
    private String deliveryMessage;
}

public enum OrderStatus {
    PENDING_PAYMENT(Set.of(PAID, CANCELLED)),
    PAID(Set.of(PREPARING, CANCELLED)),
    PREPARING(Set.of(SHIPPING, CANCELLED)),
    SHIPPING(Set.of(DELIVERED)),
    DELIVERED(Set.of(CONFIRMED, REFUND_REQUESTED)),
    CONFIRMED(Set.of()),
    CANCELLED(Set.of()),
    REFUND_REQUESTED(Set.of(REFUNDED)),
    REFUNDED(Set.of());

    private final Set<OrderStatus> allowedTransitions;

    public boolean canTransitionTo(OrderStatus newStatus) {
        return allowedTransitions.contains(newStatus);
    }
}

public enum OrderItemStatus {
    ORDERED,
    PREPARING,
    SHIPPING,
    DELIVERED,
    CONFIRMED,
    CANCEL_REQUESTED,
    CANCELLED,
    RETURN_REQUESTED,
    RETURNED
}
```

계속해서 나머지 도메인들을 작성하겠습니다.

**다음 파일**: plan-3-phase1-implementation.md에서 구체적인 구현 가이드를 제공합니다.
