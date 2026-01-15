package com.example.ecommerce.domain.inventory.dto.response;

import com.example.ecommerce.domain.inventory.entity.InventoryChangeType;
import com.example.ecommerce.domain.inventory.entity.InventoryHistory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InventoryHistoryResponse {

    private Long id;
    private Long inventoryId;
    private Long productOptionId;
    private InventoryChangeType changeType;
    private String changeTypeDescription;
    private Integer changeQuantity;
    private Integer beforeQuantity;
    private Integer afterQuantity;
    private String reason;
    private Long referenceId;
    private String referenceType;
    private LocalDateTime createdAt;

    public static InventoryHistoryResponse from(InventoryHistory history) {
        return InventoryHistoryResponse.builder()
                .id(history.getId())
                .inventoryId(history.getInventoryId())
                .productOptionId(history.getProductOptionId())
                .changeType(history.getChangeType())
                .changeTypeDescription(history.getChangeType().getDescription())
                .changeQuantity(history.getChangeQuantity())
                .beforeQuantity(history.getBeforeQuantity())
                .afterQuantity(history.getAfterQuantity())
                .reason(history.getReason())
                .referenceId(history.getReferenceId())
                .referenceType(history.getReferenceType())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
