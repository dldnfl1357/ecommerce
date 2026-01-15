package com.example.ecommerce.domain.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InventoryIncreaseRequest {

    @NotNull(message = "증가 수량은 필수입니다")
    @Min(value = 1, message = "증가 수량은 1 이상이어야 합니다")
    private Integer quantity;

    private String reason;

    @Builder
    public InventoryIncreaseRequest(Integer quantity, String reason) {
        this.quantity = quantity;
        this.reason = reason;
    }
}
