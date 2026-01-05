package com.example.ecommerce.domain.product.dto.request;

import com.example.ecommerce.domain.product.entity.DeliveryType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProductUpdateRequest {

    private Long categoryId;

    @Size(min = 2, max = 200, message = "상품명은 2자 이상 200자 이하여야 합니다")
    private String name;

    @Size(max = 10000, message = "상품 설명은 10000자 이하여야 합니다")
    private String description;

    @Min(value = 100, message = "가격은 100원 이상이어야 합니다")
    @Max(value = 100_000_000, message = "가격은 1억원 이하여야 합니다")
    private Integer basePrice;

    @Min(value = 0, message = "할인율은 0 이상이어야 합니다")
    @Max(value = 100, message = "할인율은 100 이하여야 합니다")
    private Integer discountRate;

    private DeliveryType deliveryType;

    @Min(value = 0, message = "배송비는 0 이상이어야 합니다")
    private Integer deliveryFee;

    @Min(value = 0, message = "무료배송 기준금액은 0 이상이어야 합니다")
    private Integer freeDeliveryThreshold;
}
