package com.example.ecommerce.product.domain.product.dto.response;

import com.example.ecommerce.product.domain.product.entity.ProductImage;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProductImageResponse {

    private Long id;
    private Long productId;
    private String imageUrl;
    private Integer displayOrder;
    private Boolean isMain;

    public static ProductImageResponse from(ProductImage image) {
        return ProductImageResponse.builder()
                .id(image.getId())
                .productId(image.getProductId())
                .imageUrl(image.getImageUrl())
                .displayOrder(image.getDisplayOrder())
                .isMain(image.getIsMain())
                .build();
    }
}
