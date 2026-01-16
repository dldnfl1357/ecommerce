package com.example.ecommerce.order.domain.delivery.dto.response;

import com.example.ecommerce.order.domain.delivery.entity.DeliveryHistory;
import com.example.ecommerce.order.domain.delivery.entity.DeliveryStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DeliveryHistoryResponse {

    private Long id;
    private Long deliveryId;
    private DeliveryStatus status;
    private String statusDescription;
    private String location;
    private String description;
    private LocalDateTime occurredAt;

    public static DeliveryHistoryResponse from(DeliveryHistory history) {
        return DeliveryHistoryResponse.builder()
                .id(history.getId())
                .deliveryId(history.getDeliveryId())
                .status(history.getStatus())
                .statusDescription(history.getStatus().getDescription())
                .location(history.getLocation())
                .description(history.getDescription())
                .occurredAt(history.getOccurredAt())
                .build();
    }
}
