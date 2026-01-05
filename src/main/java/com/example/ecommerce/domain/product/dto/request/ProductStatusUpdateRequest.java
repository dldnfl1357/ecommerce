package com.example.ecommerce.domain.product.dto.request;

import com.example.ecommerce.domain.product.entity.ProductStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProductStatusUpdateRequest {

    @NotNull(message = "상품 상태는 필수입니다")
    private ProductStatus status;
}
