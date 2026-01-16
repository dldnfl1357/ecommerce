package com.example.ecommerce.order.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PaymentConfirmRequest {

    @NotBlank(message = "결제 키는 필수입니다")
    private String paymentKey;

    @NotNull(message = "주문 ID는 필수입니다")
    private Long orderId;

    @NotNull(message = "결제 금액은 필수입니다")
    private BigDecimal amount;
}
