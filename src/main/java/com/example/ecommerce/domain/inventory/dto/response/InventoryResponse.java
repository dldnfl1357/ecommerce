package com.example.ecommerce.domain.inventory.dto.response;

import com.example.ecommerce.domain.inventory.entity.Inventory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InventoryResponse {

    private Long id;
    private Long productOptionId;
    private Integer quantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private Integer safetyStock;
    private Boolean isLowStock;
    private Boolean isSoldOut;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static InventoryResponse from(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productOptionId(inventory.getProductOptionId())
                .quantity(inventory.getQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .availableQuantity(inventory.getAvailableQuantity())
                .safetyStock(inventory.getSafetyStock())
                .isLowStock(inventory.isLowStock())
                .isSoldOut(inventory.isSoldOut())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }
}
