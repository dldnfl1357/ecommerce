package com.example.ecommerce.order.domain.delivery.entity;

import com.example.ecommerce.common.core.entity.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("deliveries")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Delivery extends BaseEntity {

    @Id
    private Long id;

    @Column("order_id")
    private Long orderId;

    @Column("status")
    private DeliveryStatus status;

    @Column("type")
    private DeliveryType type;

    @Column("carrier_code")
    private String carrierCode;

    @Column("carrier_name")
    private String carrierName;

    @Column("tracking_number")
    private String trackingNumber;

    @Column("recipient_name")
    private String recipientName;

    @Column("recipient_phone")
    private String recipientPhone;

    @Column("postal_code")
    private String postalCode;

    @Column("address")
    private String address;

    @Column("address_detail")
    private String addressDetail;

    @Column("delivery_request")
    private String deliveryRequest;

    @Column("estimated_delivery_date")
    private LocalDateTime estimatedDeliveryDate;

    @Column("shipped_at")
    private LocalDateTime shippedAt;

    @Column("delivered_at")
    private LocalDateTime deliveredAt;

    @Column("returned_at")
    private LocalDateTime returnedAt;

    @Column("return_reason")
    private String returnReason;

    public Delivery markAsPreparing() {
        this.status = DeliveryStatus.PREPARING;
        return this;
    }

    public Delivery ship(String carrierCode, String carrierName, String trackingNumber) {
        this.status = DeliveryStatus.SHIPPED;
        this.carrierCode = carrierCode;
        this.carrierName = carrierName;
        this.trackingNumber = trackingNumber;
        this.shippedAt = LocalDateTime.now();
        this.estimatedDeliveryDate = LocalDateTime.now().plusDays(
                this.type == DeliveryType.ROCKET ? 1 : 3
        );
        return this;
    }

    public Delivery markAsOutForDelivery() {
        this.status = DeliveryStatus.OUT_FOR_DELIVERY;
        return this;
    }

    public Delivery markAsDelivered() {
        this.status = DeliveryStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
        return this;
    }

    public Delivery requestReturn(String reason) {
        if (this.status != DeliveryStatus.DELIVERED) {
            throw new IllegalStateException("배송 완료 후에만 반품 요청이 가능합니다.");
        }
        this.status = DeliveryStatus.RETURN_REQUESTED;
        this.returnReason = reason;
        return this;
    }

    public Delivery markAsReturned() {
        this.status = DeliveryStatus.RETURNED;
        this.returnedAt = LocalDateTime.now();
        return this;
    }

    public boolean canReturn() {
        if (this.status != DeliveryStatus.DELIVERED) {
            return false;
        }
        return this.deliveredAt != null &&
               this.deliveredAt.plusDays(7).isAfter(LocalDateTime.now());
    }

    public static Delivery create(Long orderId, DeliveryType type,
                                  String recipientName, String recipientPhone,
                                  String postalCode, String address,
                                  String addressDetail, String deliveryRequest) {
        return Delivery.builder()
                .orderId(orderId)
                .status(DeliveryStatus.PENDING)
                .type(type)
                .recipientName(recipientName)
                .recipientPhone(recipientPhone)
                .postalCode(postalCode)
                .address(address)
                .addressDetail(addressDetail)
                .deliveryRequest(deliveryRequest)
                .build();
    }
}
