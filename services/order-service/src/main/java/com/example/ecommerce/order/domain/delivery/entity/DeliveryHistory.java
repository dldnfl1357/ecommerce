package com.example.ecommerce.order.domain.delivery.entity;

import com.example.ecommerce.common.core.entity.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("delivery_histories")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DeliveryHistory extends BaseEntity {

    @Id
    private Long id;

    @Column("delivery_id")
    private Long deliveryId;

    @Column("status")
    private DeliveryStatus status;

    @Column("location")
    private String location;

    @Column("description")
    private String description;

    @Column("occurred_at")
    private LocalDateTime occurredAt;

    public static DeliveryHistory create(Long deliveryId, DeliveryStatus status,
                                         String location, String description) {
        return DeliveryHistory.builder()
                .deliveryId(deliveryId)
                .status(status)
                .location(location)
                .description(description)
                .occurredAt(LocalDateTime.now())
                .build();
    }
}
