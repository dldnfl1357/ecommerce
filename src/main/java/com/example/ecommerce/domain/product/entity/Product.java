package com.example.ecommerce.domain.product.entity;

import com.example.ecommerce.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    private Long id;

    @Column("seller_id")
    private Long sellerId;

    @Column("category_id")
    private Long categoryId;

    @Column("name")
    private String name;

    @Column("description")
    private String description;

    @Column("base_price")
    private Integer basePrice;

    @Column("discount_rate")
    private Integer discountRate;

    @Column("discount_price")
    private Integer discountPrice;

    @Column("status")
    private ProductStatus status;

    @Column("delivery_type")
    private DeliveryType deliveryType;

    @Column("delivery_fee")
    private Integer deliveryFee;

    @Column("free_delivery_threshold")
    private Integer freeDeliveryThreshold;

    @Column("average_rating")
    private Double averageRating;

    @Column("review_count")
    private Integer reviewCount;

    @Column("sales_count")
    private Integer salesCount;

    @Column("view_count")
    private Integer viewCount;

    @Builder
    public Product(Long sellerId, Long categoryId, String name, String description,
                   Integer basePrice, Integer discountRate, DeliveryType deliveryType,
                   Integer deliveryFee, Integer freeDeliveryThreshold) {
        this.sellerId = sellerId;
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.discountRate = discountRate != null ? discountRate : 0;
        this.discountPrice = calculateDiscountPrice(basePrice, this.discountRate);
        this.status = ProductStatus.ON_SALE;
        this.deliveryType = deliveryType != null ? deliveryType : DeliveryType.NORMAL;
        this.deliveryFee = deliveryFee != null ? deliveryFee : 3000;
        this.freeDeliveryThreshold = freeDeliveryThreshold != null ? freeDeliveryThreshold : 30000;
        this.averageRating = 0.0;
        this.reviewCount = 0;
        this.salesCount = 0;
        this.viewCount = 0;
    }

    // ========== 상태 변경 메서드 ==========

    /**
     * 상품 판매 시작 (활성화)
     */
    public Product activate() {
        this.status = ProductStatus.ON_SALE;
        return this;
    }

    /**
     * 상품 판매 중지
     */
    public Product deactivate() {
        this.status = ProductStatus.STOP_SALE;
        return this;
    }

    /**
     * 품절 처리
     */
    public Product markAsSoldOut() {
        this.status = ProductStatus.SOLD_OUT;
        return this;
    }

    /**
     * 삭제 처리 (소프트 삭제)
     */
    public Product delete() {
        this.status = ProductStatus.DELETED;
        return this;
    }

    // ========== 통계 업데이트 메서드 ==========

    /**
     * 조회수 증가
     */
    public Product increaseViewCount() {
        this.viewCount++;
        return this;
    }

    /**
     * 판매수 증가
     */
    public Product increaseSalesCount(int quantity) {
        this.salesCount += quantity;
        return this;
    }

    /**
     * 평점 업데이트 (새 리뷰 추가 시)
     */
    public Product updateRating(double newRating) {
        double totalRating = this.averageRating * this.reviewCount + newRating;
        this.reviewCount++;
        this.averageRating = Math.round((totalRating / this.reviewCount) * 10.0) / 10.0; // 소수점 1자리
        return this;
    }

    // ========== 정보 수정 메서드 ==========

    /**
     * 상품 기본 정보 수정
     */
    public Product updateInfo(String name, String description, Long categoryId, DeliveryType deliveryType) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
        if (categoryId != null) {
            this.categoryId = categoryId;
        }
        if (deliveryType != null) {
            this.deliveryType = deliveryType;
        }
        return this;
    }

    /**
     * 가격 정보 수정
     */
    public Product updatePrice(Integer basePrice, Integer discountRate) {
        if (basePrice != null && basePrice > 0) {
            this.basePrice = basePrice;
        }
        if (discountRate != null && discountRate >= 0 && discountRate <= 100) {
            this.discountRate = discountRate;
        }
        this.discountPrice = calculateDiscountPrice(this.basePrice, this.discountRate);
        return this;
    }

    /**
     * 할인율 수정
     */
    public Product updateDiscountRate(int newDiscountRate) {
        if (newDiscountRate < 0 || newDiscountRate > 100) {
            throw new IllegalArgumentException("할인율은 0-100 사이여야 합니다.");
        }
        this.discountRate = newDiscountRate;
        this.discountPrice = calculateDiscountPrice(this.basePrice, newDiscountRate);
        return this;
    }

    /**
     * 배송 정보 수정
     */
    public Product updateDeliveryInfo(DeliveryType deliveryType, Integer deliveryFee, Integer freeDeliveryThreshold) {
        if (deliveryType != null) {
            this.deliveryType = deliveryType;
        }
        if (deliveryFee != null && deliveryFee >= 0) {
            this.deliveryFee = deliveryFee;
        }
        if (freeDeliveryThreshold != null && freeDeliveryThreshold >= 0) {
            this.freeDeliveryThreshold = freeDeliveryThreshold;
        }
        return this;
    }

    // ========== 조회/검증 메서드 ==========

    /**
     * 구매 가능 여부
     */
    public boolean isAvailable() {
        return this.status.isAvailable();
    }

    /**
     * 삭제된 상품 여부
     */
    public boolean isDeleted() {
        return this.status.isDeleted();
    }

    /**
     * 최종 판매가 조회
     */
    public int getFinalPrice() {
        return this.discountPrice;
    }

    /**
     * 해당 판매자 소유 여부
     */
    public boolean isOwnedBy(Long sellerId) {
        return this.sellerId.equals(sellerId);
    }

    /**
     * 로켓배송 상품 여부
     */
    public boolean isRocketDelivery() {
        return this.deliveryType.isRocketDelivery();
    }

    // ========== Private Helper ==========

    private static int calculateDiscountPrice(int basePrice, int discountRate) {
        if (discountRate == 0) {
            return basePrice;
        }
        return basePrice - (basePrice * discountRate / 100);
    }
}
