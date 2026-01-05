package com.example.ecommerce.domain.product.dto.request;

import com.example.ecommerce.domain.product.entity.ImageType;
import com.example.ecommerce.domain.product.entity.ProductImage;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class ProductImageRequest {

    @NotBlank(message = "이미지 URL은 필수입니다")
    @Size(max = 500, message = "이미지 URL은 500자 이하여야 합니다")
    private String imageUrl;

    @Min(value = 0, message = "정렬 순서는 0 이상이어야 합니다")
    private Integer sortOrder;

    private Boolean isThumbnail;

    private ImageType imageType;

    /**
     * Entity로 변환
     */
    public ProductImage toEntity(Long productId) {
        return ProductImage.builder()
                .productId(productId)
                .imageUrl(this.imageUrl)
                .sortOrder(this.sortOrder != null ? this.sortOrder : 0)
                .isThumbnail(this.isThumbnail != null ? this.isThumbnail : false)
                .imageType(this.imageType != null ? this.imageType : ImageType.MAIN)
                .build();
    }
}
