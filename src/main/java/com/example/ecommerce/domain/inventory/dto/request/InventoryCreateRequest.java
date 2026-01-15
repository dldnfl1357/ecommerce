package com.example.ecommerce.domain.inventory.dto.request;

import com.example.ecommerce.domain.inventory.entity.Inventory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InventoryCreateRequest {

    @NotNull(message = "상품 옵션 ID는 필수입니다")
    private Long productOptionId;

    @NotNull(message = "초기 수량은 필수입니다")
    @Min(value = 0, message = "수량은 0 이상이어야 합니다")
    private Integer quantity;

    @Min(value = 0, message = "안전 재고는 0 이상이어야 합니다")
    private Integer safetyStock;

    @Builder
    public InventoryCreateRequest(Long productOptionId, Integer quantity, Integer safetyStock) {
        this.productOptionId = productOptionId;
        this.quantity = quantity;
        this.safetyStock = safetyStock;
    }

    public Inventory toEntity() {
        return Inventory.builder()
                .productOptionId(productOptionId)
                .quantity(quantity)
                .safetyStock(safetyStock != null ? safetyStock : 10)
                .build();
    }
}
