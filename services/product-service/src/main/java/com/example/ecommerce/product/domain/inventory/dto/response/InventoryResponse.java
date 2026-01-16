package com.example.ecommerce.product.domain.inventory.dto.response;

import com.example.ecommerce.product.domain.inventory.entity.Inventory;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class InventoryResponse {

    private Long id;
    private Long productOptionId;
    private Integer quantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private Integer lowStockThreshold;
    private Boolean isLowStock;
    private Boolean isSoldOut;

    public static InventoryResponse from(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productOptionId(inventory.getProductOptionId())
                .quantity(inventory.getQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .availableQuantity(inventory.getAvailableQuantity())
                .lowStockThreshold(inventory.getLowStockThreshold())
                .isLowStock(inventory.isLowStock())
                .isSoldOut(inventory.isSoldOut())
                .build();
    }
}
