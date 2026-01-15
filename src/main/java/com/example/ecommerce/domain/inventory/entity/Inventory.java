package com.example.ecommerce.domain.inventory.entity;

import com.example.ecommerce.global.common.BaseEntity;
import com.example.ecommerce.global.exception.BusinessException;
import com.example.ecommerce.global.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("inventories")
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

    @Column("safety_stock")
    private Integer safetyStock;

    @Version
    @Column("version")
    private Long version;

    @Builder
    public Inventory(Long productOptionId, Integer quantity, Integer safetyStock) {
        this.productOptionId = productOptionId;
        this.quantity = quantity != null ? quantity : 0;
        this.reservedQuantity = 0;
        this.safetyStock = safetyStock != null ? safetyStock : 10;
    }

    // ========== 재고 조회 메서드 ==========

    /**
     * 가용 재고 조회 (전체 재고 - 예약 재고)
     */
    public int getAvailableQuantity() {
        return this.quantity - this.reservedQuantity;
    }

    /**
     * 재고 부족 여부 (안전 재고 기준)
     */
    public boolean isLowStock() {
        return getAvailableQuantity() <= this.safetyStock;
    }

    /**
     * 품절 여부
     */
    public boolean isSoldOut() {
        return getAvailableQuantity() <= 0;
    }

    /**
     * 주문 가능 여부
     */
    public boolean canOrder(int requestedQuantity) {
        return getAvailableQuantity() >= requestedQuantity;
    }

    // ========== 재고 변경 메서드 ==========

    /**
     * 재고 증가 (입고)
     */
    public Inventory increase(int amount) {
        validatePositiveAmount(amount);
        this.quantity += amount;
        return this;
    }

    /**
     * 재고 감소 (출고)
     */
    public Inventory decrease(int amount) {
        validatePositiveAmount(amount);
        if (this.quantity < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
        this.quantity -= amount;
        return this;
    }

    /**
     * 재고 예약 (주문 시)
     */
    public Inventory reserve(int amount) {
        validatePositiveAmount(amount);
        if (getAvailableQuantity() < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
        this.reservedQuantity += amount;
        return this;
    }

    /**
     * 예약 해제 (주문 취소 시)
     */
    public Inventory release(int amount) {
        validatePositiveAmount(amount);
        if (this.reservedQuantity < amount) {
            throw new BusinessException(ErrorCode.INVALID_INVENTORY_OPERATION);
        }
        this.reservedQuantity -= amount;
        return this;
    }

    /**
     * 예약 확정 (출고) - 예약 재고를 실제 출고로 전환
     */
    public Inventory confirmReservation(int amount) {
        validatePositiveAmount(amount);
        if (this.reservedQuantity < amount || this.quantity < amount) {
            throw new BusinessException(ErrorCode.INVALID_INVENTORY_OPERATION);
        }
        this.reservedQuantity -= amount;
        this.quantity -= amount;
        return this;
    }

    /**
     * 재고 직접 조정 (관리자)
     */
    public Inventory adjust(int newQuantity) {
        if (newQuantity < 0) {
            throw new BusinessException(ErrorCode.INVALID_INVENTORY_QUANTITY);
        }
        if (newQuantity < this.reservedQuantity) {
            throw new BusinessException(ErrorCode.INVALID_INVENTORY_OPERATION);
        }
        this.quantity = newQuantity;
        return this;
    }

    /**
     * 안전 재고 설정
     */
    public Inventory updateSafetyStock(int safetyStock) {
        if (safetyStock < 0) {
            throw new BusinessException(ErrorCode.INVALID_INVENTORY_QUANTITY);
        }
        this.safetyStock = safetyStock;
        return this;
    }

    // ========== Private Helper ==========

    private void validatePositiveAmount(int amount) {
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INVENTORY_QUANTITY);
        }
    }
}
