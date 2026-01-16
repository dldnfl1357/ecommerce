package com.example.ecommerce.product.domain.product.entity;

import com.example.ecommerce.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private BigDecimal basePrice;

    @Column("discount_rate")
    private Integer discountRate;

    @Column("status")
    private ProductStatus status;

    @Column("is_rocket_delivery")
    private Boolean isRocketDelivery;

    @Column("is_rocket_wow")
    private Boolean isRocketWow;

    @Column("view_count")
    private Long viewCount;

    @Column("review_count")
    private Integer reviewCount;

    @Column("rating_avg")
    private BigDecimal ratingAvg;

    @Column("deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public Product(Long sellerId, Long categoryId, String name, String description,
                   BigDecimal basePrice, Integer discountRate, Boolean isRocketDelivery,
                   Boolean isRocketWow) {
        this.sellerId = sellerId;
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.discountRate = discountRate != null ? discountRate : 0;
        this.status = ProductStatus.DRAFT;
        this.isRocketDelivery = isRocketDelivery != null ? isRocketDelivery : false;
        this.isRocketWow = isRocketWow != null ? isRocketWow : false;
        this.viewCount = 0L;
        this.reviewCount = 0;
        this.ratingAvg = BigDecimal.ZERO;
    }

    // 비즈니스 로직
    public Product publish() {
        this.status = ProductStatus.ACTIVE;
        return this;
    }

    public Product suspend() {
        this.status = ProductStatus.SUSPENDED;
        return this;
    }

    public Product delete() {
        this.status = ProductStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
        return this;
    }

    public Product incrementViewCount() {
        this.viewCount++;
        return this;
    }

    public Product updateReviewStats(int reviewCount, BigDecimal ratingAvg) {
        this.reviewCount = reviewCount;
        this.ratingAvg = ratingAvg;
        return this;
    }

    public BigDecimal getSellingPrice() {
        if (discountRate == null || discountRate == 0) {
            return basePrice;
        }
        BigDecimal discountAmount = basePrice.multiply(BigDecimal.valueOf(discountRate))
                .divide(BigDecimal.valueOf(100));
        return basePrice.subtract(discountAmount);
    }

    public boolean isActive() {
        return this.status == ProductStatus.ACTIVE;
    }

    public boolean isDeleted() {
        return this.status == ProductStatus.DELETED;
    }
}
