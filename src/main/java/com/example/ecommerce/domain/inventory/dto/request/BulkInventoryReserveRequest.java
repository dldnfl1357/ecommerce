package com.example.ecommerce.domain.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class BulkInventoryReserveRequest {

    @NotNull(message = "주문 ID는 필수입니다")
    private Long orderId;

    @NotEmpty(message = "예약 항목은 최소 1개 이상이어야 합니다")
    @Valid
    private List<ReserveItem> items;

    @Builder
    public BulkInventoryReserveRequest(Long orderId, List<ReserveItem> items) {
        this.orderId = orderId;
        this.items = items;
    }

    @Getter
    @NoArgsConstructor
    public static class ReserveItem {

        @NotNull(message = "상품 옵션 ID는 필수입니다")
        private Long productOptionId;

        @NotNull(message = "예약 수량은 필수입니다")
        private Integer quantity;

        @Builder
        public ReserveItem(Long productOptionId, Integer quantity) {
            this.productOptionId = productOptionId;
            this.quantity = quantity;
        }
    }
}
