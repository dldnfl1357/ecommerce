package com.example.ecommerce.domain.product.dto.response;

import com.example.ecommerce.domain.product.entity.ImageType;
import com.example.ecommerce.domain.product.entity.ProductImage;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProductImageResponse {

    private Long id;
    private String imageUrl;
    private Integer sortOrder;
    private Boolean isThumbnail;
    private ImageType imageType;
    private String imageTypeDisplayName;

    /**
     * Entity -> Response DTO 변환
     */
    public static ProductImageResponse from(ProductImage image) {
        return ProductImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .sortOrder(image.getSortOrder())
                .isThumbnail(image.getIsThumbnail())
                .imageType(image.getImageType())
                .imageTypeDisplayName(image.getImageType().getDisplayName())
                .build();
    }
}
