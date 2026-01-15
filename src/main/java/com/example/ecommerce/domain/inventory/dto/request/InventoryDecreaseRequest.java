package com.example.ecommerce.domain.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InventoryDecreaseRequest {

    @NotNull(message = "감소 수량은 필수입니다")
    @Min(value = 1, message = "감소 수량은 1 이상이어야 합니다")
    private Integer quantity;

    private String reason;

    private Long referenceId;

    private String referenceType;

    @Builder
    public InventoryDecreaseRequest(Integer quantity, String reason, Long referenceId, String referenceType) {
        this.quantity = quantity;
        this.reason = reason;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
    }
}
