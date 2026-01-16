package com.example.ecommerce.product.domain.inventory.entity;

import com.example.ecommerce.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("inventory")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory extends BaseEntity {

    @Id
    private Long id;

    @Column("product_option_id")
    private Long productOptionId;

    @Column("quantity")
    private Integer quantity;

    @Column("reserved_quantity")
    private Integer reservedQuantity;

    @Column("low_stock_threshold")
    private Integer lowStockThreshold;

    @Version
    @Column("version")
    private Long version;

    @Builder
    public Inventory(Long productOptionId, Integer quantity, Integer lowStockThreshold) {
        this.productOptionId = productOptionId;
        this.quantity = quantity != null ? quantity : 0;
        this.reservedQuantity = 0;
        this.lowStockThreshold = lowStockThreshold != null ? lowStockThreshold : 10;
    }

    // 가용 재고 = 전체 재고 - 예약된 재고
    public int getAvailableQuantity() {
        return this.quantity - this.reservedQuantity;
    }

    // 재고 예약
    public Inventory reserve(int amount) {
        if (getAvailableQuantity() < amount) {
            throw new IllegalStateException("재고가 부족합니다. 가용: " + getAvailableQuantity() + ", 요청: " + amount);
        }
        this.reservedQuantity += amount;
        return this;
    }

    // 예약 취소 (재고 복구)
    public Inventory releaseReservation(int amount) {
        this.reservedQuantity = Math.max(0, this.reservedQuantity - amount);
        return this;
    }

    // 예약 확정 (실제 출고)
    public Inventory confirmReservation(int amount) {
        this.quantity -= amount;
        this.reservedQuantity -= amount;
        return this;
    }

    // 재고 증가 (입고)
    public Inventory increase(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("증가량은 0 이상이어야 합니다.");
        }
        this.quantity += amount;
        return this;
    }

    // 재고 감소 (수동 조정)
    public Inventory decrease(int amount) {
        if (this.quantity < amount) {
            throw new IllegalStateException("재고가 부족합니다.");
        }
        this.quantity -= amount;
        return this;
    }

    // 재고 부족 여부
    public boolean isLowStock() {
        return getAvailableQuantity() <= this.lowStockThreshold;
    }

    // 품절 여부
    public boolean isSoldOut() {
        return getAvailableQuantity() <= 0;
    }
}
