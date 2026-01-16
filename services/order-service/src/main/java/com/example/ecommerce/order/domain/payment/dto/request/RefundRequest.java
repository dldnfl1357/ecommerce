package com.example.ecommerce.order.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RefundRequest {

    @NotNull(message = "환불 금액은 필수입니다")
    private BigDecimal amount;

    @NotBlank(message = "환불 사유는 필수입니다")
    private String reason;

    // For bank transfer refund
    private String refundBankCode;
    private String refundAccountNumber;
    private String refundAccountHolder;
}
