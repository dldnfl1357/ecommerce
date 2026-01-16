package com.example.ecommerce.order.domain.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DeliveryShipRequest {

    @NotBlank(message = "택배사 코드는 필수입니다")
    private String carrierCode;

    @NotBlank(message = "택배사명은 필수입니다")
    private String carrierName;

    @NotBlank(message = "송장번호는 필수입니다")
    private String trackingNumber;
}
