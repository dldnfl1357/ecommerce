package com.example.ecommerce.product.domain.product.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProductCreateRequest {

    @NotNull(message = "카테고리 ID는 필수입니다")
    private Long categoryId;

    @NotBlank(message = "상품명은 필수입니다")
    @Size(max = 255, message = "상품명은 255자 이하여야 합니다")
    private String name;

    private String description;

    @NotNull(message = "기본 가격은 필수입니다")
    @DecimalMin(value = "0", message = "가격은 0 이상이어야 합니다")
    private BigDecimal basePrice;

    @Min(value = 0, message = "할인율은 0 이상이어야 합니다")
    @Max(value = 100, message = "할인율은 100 이하여야 합니다")
    private Integer discountRate;

    private Boolean isRocketDelivery;
    private Boolean isRocketWow;

    @NotEmpty(message = "최소 1개의 옵션이 필요합니다")
    private List<ProductOptionRequest> options;

    private List<ProductImageRequest> images;

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class ProductOptionRequest {
        @NotBlank(message = "옵션명은 필수입니다")
        private String name;
        private BigDecimal additionalPrice;
        private Integer initialStock;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class ProductImageRequest {
        @NotBlank(message = "이미지 URL은 필수입니다")
        private String imageUrl;
        private Integer displayOrder;
        private Boolean isMain;
    }
}
