# 쿠팡 클론 프로젝트 기획서 (5/6) - Phase 3 구현 가이드

> **고급 기능 및 최적화: Coupon, Review, Seller, Recommendation (Week 17-24)**

---

## 📋 목차
1. [Week 17-18: Coupon & Review Service](#week-17-18-coupon--review-service)
2. [Week 19-20: Seller Service](#week-19-20-seller-service)
3. [Week 21-22: Recommendation Service](#week-21-22-recommendation-service)
4. [Week 23-24: 성능 최적화 & 보안](#week-23-24-성능-최적화--보안)

---

## 🎫 Week 17-18: Coupon & Review Service

### Day 1-3: Coupon Service 구현

#### 1.1 Coupon 도메인 설계

**Coupon.java**:
```java
@Entity
@Table(name = "coupons", indexes = {
    @Index(name = "idx_code", columnList = "code", unique = true),
    @Index(name = "idx_valid_period", columnList = "valid_from, valid_until")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;  // 쿠폰 코드: WELCOME2024

    @Column(nullable = false, length = 100)
    private String name;  // 쿠폰명: 신규가입 환영 쿠폰

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponType type;  // FIXED_AMOUNT, PERCENTAGE, FREE_DELIVERY

    @Column(nullable = false)
    private Integer discountValue;  // 할인 금액 또는 퍼센트

    @Column
    private Integer maxDiscountAmount;  // 최대 할인 금액 (정률 쿠폰용)

    @Column(nullable = false)
    private Integer minOrderAmount;  // 최소 주문 금액

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponTarget target;  // ALL, CATEGORY, PRODUCT, SELLER

    @Column
    private Long targetId;  // 타겟 ID (카테고리/상품/판매자)

    @Column(nullable = false)
    private LocalDateTime validFrom;

    @Column(nullable = false)
    private LocalDateTime validUntil;

    @Column(nullable = false)
    private Integer totalQuantity;  // 총 발급 수량

    @Column(nullable = false)
    private Integer issuedQuantity = 0;  // 발급된 수량

    @Column(nullable = false)
    private Integer usedQuantity = 0;  // 사용된 수량

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IssueType issueType;  // AUTO, MANUAL, FIRST_COME

    @Column(nullable = false)
    private Boolean duplicatable = false;  // 중복 발급 가능 여부

    // 비즈니스 로직
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(validFrom)
            && now.isBefore(validUntil)
            && issuedQuantity < totalQuantity;
    }

    public boolean canIssue() {
        return isValid() && issuedQuantity < totalQuantity;
    }

    public void issue() {
        if (!canIssue()) {
            throw new CouponNotIssuableException();
        }
        this.issuedQuantity++;
    }

    public void use() {
        this.usedQuantity++;
    }

    public void restore() {
        if (this.usedQuantity > 0) {
            this.usedQuantity--;
        }
    }

    public int calculateDiscount(int orderAmount) {
        if (orderAmount < minOrderAmount) {
            return 0;
        }

        return switch (type) {
            case FIXED_AMOUNT -> Math.min(discountValue, orderAmount);
            case PERCENTAGE -> {
                int discount = (int) (orderAmount * (discountValue / 100.0));
                yield maxDiscountAmount != null ?
                    Math.min(discount, maxDiscountAmount) : discount;
            }
            case FREE_DELIVERY -> 0;  // 배송비 무료는 별도 처리
        };
    }
}
```

**MemberCoupon.java** (회원별 쿠폰 발급 이력):
```java
@Entity
@Table(name = "member_coupons", indexes = {
    @Index(name = "idx_member_id", columnList = "member_id"),
    @Index(name = "idx_coupon_id", columnList = "coupon_id"),
    @Index(name = "idx_member_coupon", columnList = "member_id, coupon_id", unique = true)
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MemberCoupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponStatus status;  // ISSUED, USED, EXPIRED

    @Column
    private LocalDateTime usedAt;

    @Column
    private Long orderId;  // 사용된 주문 ID

    public static MemberCoupon issue(Member member, Coupon coupon) {
        MemberCoupon memberCoupon = new MemberCoupon();
        memberCoupon.member = member;
        memberCoupon.coupon = coupon;
        memberCoupon.status = CouponStatus.ISSUED;
        return memberCoupon;
    }

    public void use(Long orderId) {
        if (this.status != CouponStatus.ISSUED) {
            throw new CouponAlreadyUsedException();
        }
        if (!isValid()) {
            throw new CouponExpiredException();
        }

        this.status = CouponStatus.USED;
        this.usedAt = LocalDateTime.now();
        this.orderId = orderId;
    }

    public void restore() {
        if (this.status != CouponStatus.USED) {
            throw new IllegalStateException("사용된 쿠폰만 복구 가능합니다");
        }

        this.status = CouponStatus.ISSUED;
        this.usedAt = null;
        this.orderId = null;
    }

    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return now.isBefore(coupon.getValidUntil())
            && status == CouponStatus.ISSUED;
    }
}
```

#### 1.2 CouponService 구현

**CouponService.java**:
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final MemberRepository memberRepository;
    private final RedissonClient redissonClient;

    /**
     * 쿠폰 발급 (선착순)
     * Redis 분산 락을 이용한 동시성 제어
     */
    @Transactional
    public MemberCouponResponse issueCoupon(Long memberId, String couponCode) {
        String lockKey = "coupon:issue:" + couponCode;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 락 획득 (대기 5초, 점유 3초)
            boolean acquired = lock.tryLock(5, 3, TimeUnit.SECONDS);
            if (!acquired) {
                throw new CouponIssueLockException();
            }

            // 1. 쿠폰 조회
            Coupon coupon = couponRepository.findByCode(couponCode)
                .orElseThrow(() -> new CouponNotFoundException(couponCode));

            // 2. 발급 가능 여부 확인
            if (!coupon.canIssue()) {
                throw new CouponSoldOutException();
            }

            // 3. 회원 조회
            Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

            // 4. 중복 발급 체크
            if (!coupon.getDuplicatable()) {
                boolean alreadyIssued = memberCouponRepository
                    .existsByMemberIdAndCouponId(memberId, coupon.getId());
                if (alreadyIssued) {
                    throw new CouponAlreadyIssuedException();
                }
            }

            // 5. 쿠폰 발급
            coupon.issue();

            // 6. 회원 쿠폰 생성
            MemberCoupon memberCoupon = MemberCoupon.issue(member, coupon);
            memberCouponRepository.save(memberCoupon);

            return MemberCouponResponse.from(memberCoupon);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("쿠폰 발급 중 인터럽트 발생", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 쿠폰 사용
     */
    @Transactional
    public int useCoupon(Long memberCouponId, Long memberId, int orderAmount) {
        // 1. 회원 쿠폰 조회
        MemberCoupon memberCoupon = memberCouponRepository
            .findByIdAndMemberId(memberCouponId, memberId)
            .orElseThrow(() -> new MemberCouponNotFoundException());

        // 2. 유효성 검증
        if (!memberCoupon.isValid()) {
            throw new InvalidCouponException();
        }

        Coupon coupon = memberCoupon.getCoupon();

        // 3. 최소 주문 금액 확인
        if (orderAmount < coupon.getMinOrderAmount()) {
            throw new InsufficientOrderAmountException(coupon.getMinOrderAmount());
        }

        // 4. 할인 금액 계산
        int discountAmount = coupon.calculateDiscount(orderAmount);

        // 5. 쿠폰 사용 처리는 주문 확정 시점에 (여기서는 검증만)
        return discountAmount;
    }

    /**
     * 쿠폰 사용 확정 (주문 완료 시)
     */
    @Transactional
    public void confirmCouponUsage(Long memberCouponId, Long orderId) {
        MemberCoupon memberCoupon = memberCouponRepository.findById(memberCouponId)
            .orElseThrow(() -> new MemberCouponNotFoundException());

        memberCoupon.use(orderId);
        memberCoupon.getCoupon().use();
    }

    /**
     * 쿠폰 복구 (주문 취소 시)
     */
    @Transactional
    public void restoreCoupon(Long memberCouponId) {
        MemberCoupon memberCoupon = memberCouponRepository.findById(memberCouponId)
            .orElseThrow(() -> new MemberCouponNotFoundException());

        memberCoupon.restore();
        memberCoupon.getCoupon().restore();
    }

    /**
     * 내 쿠폰 목록 조회
     */
    public Page<MemberCouponResponse> getMyCoupons(
        Long memberId,
        CouponStatus status,
        Pageable pageable
    ) {
        Page<MemberCoupon> memberCoupons = memberCouponRepository
            .findByMemberIdAndStatus(memberId, status, pageable);

        return memberCoupons.map(MemberCouponResponse::from);
    }

    /**
     * 자동 쿠폰 발급 (이벤트 기반)
     */
    @Transactional
    @EventListener
    public void issueWelcomeCoupon(MemberCreatedEvent event) {
        // 신규 회원 환영 쿠폰 자동 발급
        List<Coupon> welcomeCoupons = couponRepository
            .findByIssueTypeAndValid(IssueType.AUTO, LocalDateTime.now());

        for (Coupon coupon : welcomeCoupons) {
            try {
                issueCoupon(event.getMemberId(), coupon.getCode());
            } catch (Exception e) {
                log.error("자동 쿠폰 발급 실패: memberId={}, couponCode={}",
                    event.getMemberId(), coupon.getCode(), e);
            }
        }
    }
}
```

### Day 4-5: Review Service 구현

**ReviewService.java**:
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final S3FileUploader s3FileUploader;
    private final ReviewEventPublisher eventPublisher;

    /**
     * 리뷰 작성
     */
    @Transactional
    public ReviewResponse createReview(
        Long memberId,
        ReviewCreateRequest request
    ) {
        // 1. 주문 조회 및 검증
        Order order = orderRepository.findById(request.getOrderId())
            .orElseThrow(() -> new OrderNotFoundException(request.getOrderId()));

        // 권한 확인
        if (!order.isOwnedBy(memberId)) {
            throw new UnauthorizedOrderAccessException();
        }

        // 구매 확정 여부 확인
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new ReviewNotAllowedException("구매 확정 후 리뷰 작성 가능");
        }

        // 2. 주문 항목 조회
        OrderItem orderItem = order.findOrderItem(request.getOrderItemId());
        if (orderItem == null) {
            throw new OrderItemNotFoundException(request.getOrderItemId());
        }

        // 3. 중복 리뷰 확인
        if (reviewRepository.existsByOrderItemId(orderItem.getId())) {
            throw new DuplicateReviewException();
        }

        // 4. 상품 조회
        Product product = orderItem.getProductOption().getProduct();

        // 5. 리뷰 생성
        Review review = Review.builder()
            .member(order.getMember())
            .product(product)
            .orderItem(orderItem)
            .rating(request.getRating())
            .content(request.getContent())
            .build();

        // 6. 리뷰 이미지 업로드
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            for (MultipartFile image : request.getImages()) {
                String imageUrl = s3FileUploader.upload(image, "reviews");

                ReviewImage reviewImage = ReviewImage.builder()
                    .review(review)
                    .imageUrl(imageUrl)
                    .sortOrder(request.getImages().indexOf(image))
                    .build();

                review.addImage(reviewImage);
            }
        }

        reviewRepository.save(review);

        // 7. 상품 평점 업데이트
        product.updateRating(request.getRating());

        // 8. 리뷰 작성 적립금 지급
        int reviewPoint = calculateReviewPoint(review);
        order.getMember().earnPoint(reviewPoint);

        // 9. 이벤트 발행
        eventPublisher.publishReviewCreated(review);

        return ReviewResponse.from(review);
    }

    /**
     * 리뷰 수정
     */
    @Transactional
    public ReviewResponse updateReview(
        Long memberId,
        Long reviewId,
        ReviewUpdateRequest request
    ) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        // 권한 확인
        if (!review.isOwnedBy(memberId)) {
            throw new UnauthorizedReviewAccessException();
        }

        // 수정 가능 기간 확인 (작성 후 30일 이내)
        if (!review.isModifiable()) {
            throw new ReviewNotModifiableException();
        }

        // 리뷰 수정
        review.update(request.getRating(), request.getContent());

        return ReviewResponse.from(review);
    }

    /**
     * 리뷰 삭제
     */
    @Transactional
    public void deleteReview(Long memberId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        if (!review.isOwnedBy(memberId)) {
            throw new UnauthorizedReviewAccessException();
        }

        // 소프트 삭제
        review.delete();

        // 상품 평점 재계산
        Product product = review.getProduct();
        recalculateProductRating(product);
    }

    /**
     * 상품별 리뷰 조회
     */
    public Page<ReviewResponse> getProductReviews(
        Long productId,
        ReviewSortType sort,
        Pageable pageable
    ) {
        Page<Review> reviews = switch (sort) {
            case LATEST -> reviewRepository.findByProductIdOrderByCreatedAtDesc(
                productId, pageable
            );
            case RATING_HIGH -> reviewRepository.findByProductIdOrderByRatingDesc(
                productId, pageable
            );
            case RATING_LOW -> reviewRepository.findByProductIdOrderByRatingAsc(
                productId, pageable
            );
            case HELPFUL -> reviewRepository.findByProductIdOrderByHelpfulCountDesc(
                productId, pageable
            );
        };

        return reviews.map(ReviewResponse::from);
    }

    /**
     * 리뷰 도움됨 표시
     */
    @Transactional
    public void markAsHelpful(Long memberId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        // 중복 체크
        if (reviewRepository.existsHelpful(reviewId, memberId)) {
            throw new DuplicateHelpfulException();
        }

        review.increaseHelpfulCount();

        // 도움됨 이력 저장 (별도 테이블)
        reviewHelpfulRepository.save(
            ReviewHelpful.of(reviewId, memberId)
        );
    }

    private int calculateReviewPoint(Review review) {
        int point = 500;  // 기본 적립금

        // 포토 리뷰 추가 적립
        if (review.hasImages()) {
            point += 500;
        }

        // 50자 이상 추가 적립
        if (review.getContent().length() >= 50) {
            point += 300;
        }

        return point;
    }

    private void recalculateProductRating(Product product) {
        Double avgRating = reviewRepository.calculateAverageRating(product.getId());
        Integer reviewCount = reviewRepository.countByProductId(product.getId());

        product.updateRatingInfo(
            avgRating != null ? avgRating : 0.0,
            reviewCount != null ? reviewCount : 0
        );
    }
}
```

---

## 🏪 Week 19-20: Seller Service

### Day 1-3: 판매자 관리 시스템

**SellerService.java**:
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerService {

    private final SellerRepository sellerRepository;
    private final SellerSettlementRepository settlementRepository;

    /**
     * 판매자 등록
     */
    @Transactional
    public SellerResponse registerSeller(SellerRegisterRequest request) {
        // 1. 사업자 등록번호 중복 체크
        if (sellerRepository.existsByBusinessNumber(request.getBusinessNumber())) {
            throw new DuplicateBusinessNumberException();
        }

        // 2. 판매자 생성
        Seller seller = Seller.builder()
            .businessName(request.getBusinessName())
            .businessNumber(request.getBusinessNumber())
            .representativeName(request.getRepresentativeName())
            .email(request.getEmail())
            .phone(request.getPhone())
            .address(request.getAddress())
            .status(SellerStatus.PENDING)  // 승인 대기
            .build();

        sellerRepository.save(seller);

        return SellerResponse.from(seller);
    }

    /**
     * 판매자 승인
     */
    @Transactional
    public void approveSeller(Long sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
            .orElseThrow(() -> new SellerNotFoundException(sellerId));

        seller.approve();
    }

    /**
     * 판매 통계 조회
     */
    public SellerStatisticsResponse getStatistics(
        Long sellerId,
        LocalDate startDate,
        LocalDate endDate
    ) {
        Seller seller = sellerRepository.findById(sellerId)
            .orElseThrow(() -> new SellerNotFoundException(sellerId));

        // 1. 총 판매액
        Integer totalSales = calculateTotalSales(sellerId, startDate, endDate);

        // 2. 총 주문 건수
        Long totalOrders = countTotalOrders(sellerId, startDate, endDate);

        // 3. 카테고리별 판매액
        Map<String, Integer> salesByCategory = getSalesByCategory(
            sellerId, startDate, endDate
        );

        // 4. 일별 판매액
        List<DailySales> dailySales = getDailySales(sellerId, startDate, endDate);

        // 5. 베스트 상품
        List<ProductSales> topProducts = getTopProducts(sellerId, startDate, endDate, 10);

        return SellerStatisticsResponse.builder()
            .totalSales(totalSales)
            .totalOrders(totalOrders)
            .salesByCategory(salesByCategory)
            .dailySales(dailySales)
            .topProducts(topProducts)
            .build();
    }

    /**
     * 정산 처리
     */
    @Transactional
    public SellerSettlementResponse createSettlement(
        Long sellerId,
        LocalDate settlementDate
    ) {
        Seller seller = sellerRepository.findById(sellerId)
            .orElseThrow(() -> new SellerNotFoundException(sellerId));

        // 1. 정산 기간 계산 (전월 1일 ~ 말일)
        LocalDate startDate = settlementDate.withDayOfMonth(1).minusMonths(1);
        LocalDate endDate = startDate.with(TemporalAdjusters.lastDayOfMonth());

        // 2. 정산 대상 주문 조회 (구매 확정된 주문)
        List<Order> orders = orderRepository.findConfirmedOrdersBySeller(
            sellerId,
            startDate.atStartOfDay(),
            endDate.atTime(23, 59, 59)
        );

        // 3. 정산 금액 계산
        int totalSalesAmount = orders.stream()
            .mapToInt(Order::getFinalPrice)
            .sum();

        // 4. 수수료 계산
        int commissionAmount = calculateCommission(orders);

        // 5. 배송비 계산
        int deliveryFeeAmount = orders.stream()
            .mapToInt(Order::getTotalDeliveryFee)
            .sum();

        // 6. 정산 금액 = 판매 금액 - 수수료
        int settlementAmount = totalSalesAmount - commissionAmount;

        // 7. 정산 생성
        SellerSettlement settlement = SellerSettlement.builder()
            .seller(seller)
            .settlementDate(settlementDate)
            .startDate(startDate)
            .endDate(endDate)
            .totalSalesAmount(totalSalesAmount)
            .commissionAmount(commissionAmount)
            .deliveryFeeAmount(deliveryFeeAmount)
            .settlementAmount(settlementAmount)
            .orderCount(orders.size())
            .status(SettlementStatus.PENDING)
            .build();

        settlementRepository.save(settlement);

        return SellerSettlementResponse.from(settlement);
    }

    private int calculateCommission(List<Order> orders) {
        return orders.stream()
            .flatMap(order -> order.getOrderItems().stream())
            .mapToInt(item -> {
                Product product = item.getProductOption().getProduct();
                Category category = product.getCategory();
                double commissionRate = category.getCommissionRate();  // 카테고리별 수수료율
                return (int) (item.getTotalPrice() * commissionRate);
            })
            .sum();
    }
}
```

---

## 🤖 Week 21-22: Recommendation Service (AI 추천)

### Day 1-5: 추천 시스템 구현

**RecommendationService.java**:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RecommendationEngine recommendationEngine;

    /**
     * 개인화 추천 (협업 필터링)
     */
    public List<ProductResponse> getPersonalizedRecommendations(
        Long memberId,
        int limit
    ) {
        String cacheKey = "recommendation:personalized:" + memberId;

        // 1. 캐시 확인
        List<Long> cachedProductIds = getCachedRecommendations(cacheKey);
        if (cachedProductIds != null && !cachedProductIds.isEmpty()) {
            return getProducts(cachedProductIds);
        }

        // 2. 사용자 구매 이력 조회
        List<Long> purchasedProductIds = orderRepository
            .findConfirmedOrdersByMemberId(memberId).stream()
            .flatMap(order -> order.getOrderItems().stream())
            .map(item -> item.getProductOption().getProduct().getId())
            .distinct()
            .collect(Collectors.toList());

        // 3. 유사 사용자 찾기
        List<Long> similarMemberIds = findSimilarMembers(memberId, purchasedProductIds);

        // 4. 유사 사용자가 구매한 상품 중 내가 구매하지 않은 상품 추천
        List<Long> recommendedProductIds = orderRepository
            .findConfirmedOrdersByMemberIdIn(similarMemberIds).stream()
            .flatMap(order -> order.getOrderItems().stream())
            .map(item -> item.getProductOption().getProduct().getId())
            .filter(productId -> !purchasedProductIds.contains(productId))
            .distinct()
            .limit(limit)
            .collect(Collectors.toList());

        // 5. 캐시 저장 (1시간)
        cacheRecommendations(cacheKey, recommendedProductIds, 3600);

        return getProducts(recommendedProductIds);
    }

    /**
     * 상품 기반 추천 (연관 상품)
     */
    public List<ProductResponse> getRelatedProducts(Long productId, int limit) {
        String cacheKey = "recommendation:related:" + productId;

        // 1. 캐시 확인
        List<Long> cachedProductIds = getCachedRecommendations(cacheKey);
        if (cachedProductIds != null && !cachedProductIds.isEmpty()) {
            return getProducts(cachedProductIds);
        }

        // 2. 현재 상품 조회
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));

        // 3. 같은 카테고리의 인기 상품
        List<Long> relatedProductIds = productRepository
            .findByCategoryIdAndStatusOrderBySalesCountDesc(
                product.getCategory().getId(),
                ProductStatus.ON_SALE,
                PageRequest.of(0, limit)
            ).stream()
            .map(Product::getId)
            .filter(id -> !id.equals(productId))
            .collect(Collectors.toList());

        // 4. 함께 구매한 상품 (빈발 패턴 마이닝)
        List<Long> frequentlyBoughtTogether = findFrequentlyBoughtTogether(productId, limit);

        // 5. 두 결과 병합
        Set<Long> combined = new LinkedHashSet<>(relatedProductIds);
        combined.addAll(frequentlyBoughtTogether);

        List<Long> finalRecommendations = combined.stream()
            .limit(limit)
            .collect(Collectors.toList());

        // 6. 캐시 저장
        cacheRecommendations(cacheKey, finalRecommendations, 7200);

        return getProducts(finalRecommendations);
    }

    /**
     * 실시간 인기 상품
     */
    public List<ProductResponse> getTrendingProducts(int limit) {
        String cacheKey = "recommendation:trending";

        // Redis Sorted Set 사용 (조회수 기반)
        Set<Object> productIds = redisTemplate.opsForZSet()
            .reverseRange(cacheKey, 0, limit - 1);

        if (productIds != null && !productIds.isEmpty()) {
            List<Long> ids = productIds.stream()
                .map(id -> Long.parseLong(id.toString()))
                .collect(Collectors.toList());
            return getProducts(ids);
        }

        // 최근 24시간 판매량 기준
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<Product> trending = productRepository
            .findTrendingProducts(since, PageRequest.of(0, limit));

        return trending.stream()
            .map(ProductResponse::from)
            .collect(Collectors.toList());
    }

    /**
     * 카테고리별 인기 상품
     */
    public List<ProductResponse> getCategoryBestSellers(Long categoryId, int limit) {
        return productRepository
            .findByCategoryIdAndStatusOrderBySalesCountDesc(
                categoryId,
                ProductStatus.ON_SALE,
                PageRequest.of(0, limit)
            ).stream()
            .map(ProductResponse::from)
            .collect(Collectors.toList());
    }

    /**
     * 최근 본 상품 기반 추천
     */
    public List<ProductResponse> getRecentlyViewedBasedRecommendations(
        Long memberId,
        int limit
    ) {
        // Redis에서 최근 본 상품 조회
        String key = "member:recent_viewed:" + memberId;
        List<Object> recentViewed = redisTemplate.opsForList()
            .range(key, 0, 9);  // 최근 10개

        if (recentViewed == null || recentViewed.isEmpty()) {
            return Collections.emptyList();
        }

        // 최근 본 상품들의 카테고리 기반 추천
        List<Long> viewedProductIds = recentViewed.stream()
            .map(id -> Long.parseLong(id.toString()))
            .collect(Collectors.toList());

        List<Product> viewedProducts = productRepository.findAllById(viewedProductIds);

        // 해당 카테고리들의 인기 상품 추천
        Set<Long> categoryIds = viewedProducts.stream()
            .map(p -> p.getCategory().getId())
            .collect(Collectors.toSet());

        return productRepository.findPopularByCategoryIds(
            categoryIds,
            viewedProductIds,  // 이미 본 상품 제외
            PageRequest.of(0, limit)
        ).stream()
            .map(ProductResponse::from)
            .collect(Collectors.toList());
    }

    /**
     * 유사 사용자 찾기 (코사인 유사도)
     */
    private List<Long> findSimilarMembers(Long memberId, List<Long> purchasedProductIds) {
        // 간단한 버전: 같은 상품을 많이 구매한 사용자
        return orderRepository.findMembersWhoPurchasedProducts(purchasedProductIds).stream()
            .filter(id -> !id.equals(memberId))
            .limit(10)
            .collect(Collectors.toList());
    }

    /**
     * 함께 구매한 상품 (Apriori 알고리즘 간소화 버전)
     */
    private List<Long> findFrequentlyBoughtTogether(Long productId, int limit) {
        return orderRepository.findFrequentlyBoughtTogether(productId, limit);
    }

    private void cacheRecommendations(String key, List<Long> productIds, int ttl) {
        redisTemplate.opsForValue().set(key, productIds, ttl, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unchecked")
    private List<Long> getCachedRecommendations(String key) {
        return (List<Long>) redisTemplate.opsForValue().get(key);
    }

    private List<ProductResponse> getProducts(List<Long> productIds) {
        return productRepository.findAllById(productIds).stream()
            .map(ProductResponse::from)
            .collect(Collectors.toList());
    }
}
```

---

## ⚡ Week 23-24: 성능 최적화 & 보안

### Day 1-3: 성능 최적화

#### 4.1 캐싱 전략

**CacheConfig.java**:
```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()
                )
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()
                )
            );

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 상품 캐시 (1시간)
        cacheConfigurations.put("products",
            config.entryTtl(Duration.ofHours(1)));

        // 카테고리 캐시 (24시간)
        cacheConfigurations.put("categories",
            config.entryTtl(Duration.ofHours(24)));

        // 회원 캐시 (30분)
        cacheConfigurations.put("members",
            config.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
}
```

#### 4.2 데이터베이스 최적화

**쿼리 최적화**:
```java
// N+1 문제 해결
@Query("SELECT p FROM Product p " +
       "LEFT JOIN FETCH p.category " +
       "LEFT JOIN FETCH p.seller " +
       "LEFT JOIN FETCH p.options o " +
       "LEFT JOIN FETCH o.inventory " +
       "WHERE p.id = :productId")
Optional<Product> findByIdWithDetails(@Param("productId") Long productId);

// Batch Size 설정
@BatchSize(size = 100)
@OneToMany(mappedBy = "product")
private List<ProductOption> options;

// 인덱스 최적화
@Table(indexes = {
    @Index(name = "idx_product_category_status",
           columnList = "category_id, status"),
    @Index(name = "idx_product_created_at",
           columnList = "created_at DESC"),
    @Index(name = "idx_product_sales_count",
           columnList = "sales_count DESC")
})
```

### Day 4-5: 보안 강화

**SecurityConfig.java**:
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/products/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter(),
                UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
```

**다음 파일**: plan-6-deployment.md에서 배포 및 운영을 다룹니다.
