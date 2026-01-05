package com.example.ecommerce.domain.product.entity;

import com.example.ecommerce.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("product_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductOption extends BaseEntity {

    @Id
    private Long id;

    @Column("product_id")
    private Long productId;

    @Column("option_name")
    private String optionName;

    @Column("option1")
    private String option1;

    @Column("option2")
    private String option2;

    @Column("add_price")
    private Integer addPrice;

    @Column("stock_quantity")
    private Integer stockQuantity;

    @Column("is_available")
    private Boolean isAvailable;

    @Builder
    public ProductOption(Long productId, String optionName, String option1,
                         String option2, Integer addPrice, Integer stockQuantity) {
        this.productId = productId;
        this.optionName = optionName;
        this.option1 = option1;
        this.option2 = option2;
        this.addPrice = addPrice != null ? addPrice : 0;
        this.stockQuantity = stockQuantity != null ? stockQuantity : 0;
        this.isAvailable = true;
    }

    // ========== 상태 변경 메서드 ==========

    /**
     * 옵션 활성화
     */
    public ProductOption enable() {
        this.isAvailable = true;
        return this;
    }

    /**
     * 옵션 비활성화
     */
    public ProductOption disable() {
        this.isAvailable = false;
        return this;
    }

    // ========== 재고 관리 메서드 ==========

    /**
     * 재고 설정
     */
    public ProductOption updateStock(int quantity) {
        this.stockQuantity = quantity;
        if (quantity <= 0) {
            this.isAvailable = false;
        }
        return this;
    }

    /**
     * 재고 차감
     */
    public ProductOption decreaseStock(int amount) {
        if (this.stockQuantity < amount) {
            throw new IllegalStateException("재고가 부족합니다.");
        }
        this.stockQuantity -= amount;
        if (this.stockQuantity == 0) {
            this.isAvailable = false;
        }
        return this;
    }

    /**
     * 재고 증가
     */
    public ProductOption increaseStock(int amount) {
        this.stockQuantity += amount;
        if (this.stockQuantity > 0 && !this.isAvailable) {
            this.isAvailable = true;
        }
        return this;
    }

    // ========== 정보 수정 메서드 ==========

    /**
     * 옵션 정보 수정
     */
    public ProductOption updateInfo(String optionName, String option1, String option2, Integer addPrice) {
        if (optionName != null && !optionName.isBlank()) {
            this.optionName = optionName;
        }
        if (option1 != null) {
            this.option1 = option1;
        }
        if (option2 != null) {
            this.option2 = option2;
        }
        if (addPrice != null && addPrice >= 0) {
            this.addPrice = addPrice;
        }
        return this;
    }

    // ========== 조회/검증 메서드 ==========

    /**
     * 재고 있음 여부
     */
    public boolean hasStock() {
        return this.stockQuantity > 0;
    }

    /**
     * 요청 수량만큼 재고 있음 여부
     */
    public boolean hasEnoughStock(int requestedQuantity) {
        return this.stockQuantity >= requestedQuantity;
    }

    /**
     * 구매 가능 여부
     */
    public boolean canPurchase() {
        return Boolean.TRUE.equals(this.isAvailable) && hasStock();
    }

    /**
     * 최종 가격 계산 (기본가격 + 추가가격)
     */
    public int calculateFinalPrice(int basePrice) {
        return basePrice + this.addPrice;
    }
}
