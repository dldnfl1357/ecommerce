package com.example.ecommerce.order.domain.payment.dto.request;

import com.example.ecommerce.order.domain.payment.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PaymentRequest {

    @NotNull(message = "주문 ID는 필수입니다")
    private Long orderId;

    @NotNull(message = "결제 수단은 필수입니다")
    private PaymentMethod method;

    @NotNull(message = "결제 금액은 필수입니다")
    private BigDecimal amount;

    // Card payment fields
    private String cardNumber;
    private String cardExpiry;
    private String cardCvc;
    private Integer installmentMonths;

    // Virtual account fields
    private String bankCode;
}
