package com.example.ecommerce.order.domain.delivery.dto.request;

import com.example.ecommerce.order.domain.delivery.entity.DeliveryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DeliveryCreateRequest {

    @NotNull(message = "주문 ID는 필수입니다")
    private Long orderId;

    @NotNull(message = "배송 유형은 필수입니다")
    private DeliveryType type;

    @NotBlank(message = "수령인 이름은 필수입니다")
    private String recipientName;

    @NotBlank(message = "수령인 연락처는 필수입니다")
    private String recipientPhone;

    @NotBlank(message = "우편번호는 필수입니다")
    private String postalCode;

    @NotBlank(message = "주소는 필수입니다")
    private String address;

    private String addressDetail;

    private String deliveryRequest;
}
