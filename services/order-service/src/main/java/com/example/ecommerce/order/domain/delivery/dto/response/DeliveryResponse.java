package com.example.ecommerce.order.domain.delivery.dto.response;

import com.example.ecommerce.order.domain.delivery.entity.Delivery;
import com.example.ecommerce.order.domain.delivery.entity.DeliveryStatus;
import com.example.ecommerce.order.domain.delivery.entity.DeliveryType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DeliveryResponse {

    private Long id;
    private Long orderId;
    private DeliveryStatus status;
    private String statusDescription;
    private DeliveryType type;
    private String typeDescription;
    private String carrierCode;
    private String carrierName;
    private String trackingNumber;
    private String recipientName;
    private String recipientPhone;
    private String postalCode;
    private String address;
    private String addressDetail;
    private String deliveryRequest;
    private LocalDateTime estimatedDeliveryDate;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private Boolean canReturn;
    private List<DeliveryHistoryResponse> histories;

    public static DeliveryResponse from(Delivery delivery) {
        return DeliveryResponse.builder()
                .id(delivery.getId())
                .orderId(delivery.getOrderId())
                .status(delivery.getStatus())
                .statusDescription(delivery.getStatus().getDescription())
                .type(delivery.getType())
                .typeDescription(delivery.getType().getDescription())
                .carrierCode(delivery.getCarrierCode())
                .carrierName(delivery.getCarrierName())
                .trackingNumber(delivery.getTrackingNumber())
                .recipientName(delivery.getRecipientName())
                .recipientPhone(maskPhone(delivery.getRecipientPhone()))
                .postalCode(delivery.getPostalCode())
                .address(delivery.getAddress())
                .addressDetail(delivery.getAddressDetail())
                .deliveryRequest(delivery.getDeliveryRequest())
                .estimatedDeliveryDate(delivery.getEstimatedDeliveryDate())
                .shippedAt(delivery.getShippedAt())
                .deliveredAt(delivery.getDeliveredAt())
                .canReturn(delivery.canReturn())
                .build();
    }

    public static DeliveryResponse from(Delivery delivery, List<DeliveryHistoryResponse> histories) {
        DeliveryResponse response = from(delivery);
        return DeliveryResponse.builder()
                .id(response.getId())
                .orderId(response.getOrderId())
                .status(response.getStatus())
                .statusDescription(response.getStatusDescription())
                .type(response.getType())
                .typeDescription(response.getTypeDescription())
                .carrierCode(response.getCarrierCode())
                .carrierName(response.getCarrierName())
                .trackingNumber(response.getTrackingNumber())
                .recipientName(response.getRecipientName())
                .recipientPhone(response.getRecipientPhone())
                .postalCode(response.getPostalCode())
                .address(response.getAddress())
                .addressDetail(response.getAddressDetail())
                .deliveryRequest(response.getDeliveryRequest())
                .estimatedDeliveryDate(response.getEstimatedDeliveryDate())
                .shippedAt(response.getShippedAt())
                .deliveredAt(response.getDeliveredAt())
                .canReturn(response.getCanReturn())
                .histories(histories)
                .build();
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return phone;
        }
        return phone.substring(0, phone.length() - 4) + "****";
    }
}
