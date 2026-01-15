package com.example.ecommerce.domain.inventory.dto.response;

import com.example.ecommerce.domain.inventory.entity.Inventory;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InventoryStockResponse {

    private Long productOptionId;
    private Integer availableQuantity;
    private Boolean canOrder;

    public static InventoryStockResponse from(Inventory inventory) {
        return InventoryStockResponse.builder()
                .productOptionId(inventory.getProductOptionId())
                .availableQuantity(inventory.getAvailableQuantity())
                .canOrder(inventory.getAvailableQuantity() > 0)
                .build();
    }

    public static InventoryStockResponse from(Inventory inventory, int requestedQuantity) {
        return InventoryStockResponse.builder()
                .productOptionId(inventory.getProductOptionId())
                .availableQuantity(inventory.getAvailableQuantity())
                .canOrder(inventory.canOrder(requestedQuantity))
                .build();
    }
}
