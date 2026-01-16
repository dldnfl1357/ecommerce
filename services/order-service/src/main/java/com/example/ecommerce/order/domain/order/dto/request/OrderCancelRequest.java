package com.example.ecommerce.order.domain.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class OrderCancelRequest {

    @NotBlank(message = "취소 사유는 필수입니다")
    private String reason;
}
