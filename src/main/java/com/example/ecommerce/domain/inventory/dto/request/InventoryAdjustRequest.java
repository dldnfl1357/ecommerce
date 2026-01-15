package com.example.ecommerce.domain.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InventoryAdjustRequest {

    @NotNull(message = "조정 수량은 필수입니다")
    @Min(value = 0, message = "수량은 0 이상이어야 합니다")
    private Integer quantity;

    @NotBlank(message = "조정 사유는 필수입니다")
    private String reason;

    @Builder
    public InventoryAdjustRequest(Integer quantity, String reason) {
        this.quantity = quantity;
        this.reason = reason;
    }
}
