package com.example.ecommerce.product.domain.product.dto.response;

import com.example.ecommerce.product.domain.product.entity.ProductOption;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProductOptionResponse {

    private Long id;
    private Long productId;
    private String name;
    private BigDecimal additionalPrice;
    private Boolean isActive;

    public static ProductOptionResponse from(ProductOption option) {
        return ProductOptionResponse.builder()
                .id(option.getId())
                .productId(option.getProductId())
                .name(option.getName())
                .additionalPrice(option.getAdditionalPrice())
                .isActive(option.getIsActive())
                .build();
    }
}
