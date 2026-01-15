package com.example.ecommerce.domain.inventory.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("inventory_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryHistory {

    @Id
    private Long id;

    @Column("inventory_id")
    private Long inventoryId;

    @Column("product_option_id")
    private Long productOptionId;

    @Column("change_type")
    private InventoryChangeType changeType;

    @Column("change_quantity")
    private Integer changeQuantity;

    @Column("before_quantity")
    private Integer beforeQuantity;

    @Column("after_quantity")
    private Integer afterQuantity;

    @Column("reason")
    private String reason;

    @Column("reference_id")
    private Long referenceId;

    @Column("reference_type")
    private String referenceType;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @Builder
    public InventoryHistory(Long inventoryId, Long productOptionId, InventoryChangeType changeType,
                            Integer changeQuantity, Integer beforeQuantity, Integer afterQuantity,
                            String reason, Long referenceId, String referenceType) {
        this.inventoryId = inventoryId;
        this.productOptionId = productOptionId;
        this.changeType = changeType;
        this.changeQuantity = changeQuantity;
        this.beforeQuantity = beforeQuantity;
        this.afterQuantity = afterQuantity;
        this.reason = reason;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
    }

    // ========== Static Factory Methods ==========

    public static InventoryHistory createIncreaseHistory(Inventory inventory, int changeQuantity,
                                                         int beforeQuantity, String reason) {
        return InventoryHistory.builder()
                .inventoryId(inventory.getId())
                .productOptionId(inventory.getProductOptionId())
                .changeType(InventoryChangeType.INCREASE)
                .changeQuantity(changeQuantity)
                .beforeQuantity(beforeQuantity)
                .afterQuantity(inventory.getQuantity())
                .reason(reason)
                .build();
    }

    public static InventoryHistory createDecreaseHistory(Inventory inventory, int changeQuantity,
                                                         int beforeQuantity, String reason,
                                                         Long referenceId, String referenceType) {
        return InventoryHistory.builder()
                .inventoryId(inventory.getId())
                .productOptionId(inventory.getProductOptionId())
                .changeType(InventoryChangeType.DECREASE)
                .changeQuantity(changeQuantity)
                .beforeQuantity(beforeQuantity)
                .afterQuantity(inventory.getQuantity())
                .reason(reason)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build();
    }

    public static InventoryHistory createReserveHistory(Inventory inventory, int changeQuantity,
                                                        int beforeAvailable, Long orderId) {
        return InventoryHistory.builder()
                .inventoryId(inventory.getId())
                .productOptionId(inventory.getProductOptionId())
                .changeType(InventoryChangeType.RESERVE)
                .changeQuantity(changeQuantity)
                .beforeQuantity(beforeAvailable)
                .afterQuantity(inventory.getAvailableQuantity())
                .reason("주문 예약")
                .referenceId(orderId)
                .referenceType("ORDER")
                .build();
    }

    public static InventoryHistory createReleaseHistory(Inventory inventory, int changeQuantity,
                                                        int beforeAvailable, Long orderId, String reason) {
        return InventoryHistory.builder()
                .inventoryId(inventory.getId())
                .productOptionId(inventory.getProductOptionId())
                .changeType(InventoryChangeType.RELEASE)
                .changeQuantity(changeQuantity)
                .beforeQuantity(beforeAvailable)
                .afterQuantity(inventory.getAvailableQuantity())
                .reason(reason)
                .referenceId(orderId)
                .referenceType("ORDER")
                .build();
    }

    public static InventoryHistory createAdjustHistory(Inventory inventory, int beforeQuantity,
                                                       int afterQuantity, String reason) {
        return InventoryHistory.builder()
                .inventoryId(inventory.getId())
                .productOptionId(inventory.getProductOptionId())
                .changeType(InventoryChangeType.ADJUST)
                .changeQuantity(afterQuantity - beforeQuantity)
                .beforeQuantity(beforeQuantity)
                .afterQuantity(afterQuantity)
                .reason(reason)
                .build();
    }

    public static InventoryHistory createReturnHistory(Inventory inventory, int changeQuantity,
                                                       int beforeQuantity, Long orderId) {
        return InventoryHistory.builder()
                .inventoryId(inventory.getId())
                .productOptionId(inventory.getProductOptionId())
                .changeType(InventoryChangeType.RETURN)
                .changeQuantity(changeQuantity)
                .beforeQuantity(beforeQuantity)
                .afterQuantity(inventory.getQuantity())
                .reason("반품 입고")
                .referenceId(orderId)
                .referenceType("ORDER")
                .build();
    }
}
