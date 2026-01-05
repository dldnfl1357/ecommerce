package com.example.ecommerce.domain.product.dto.response;

import com.example.ecommerce.domain.product.entity.DeliveryType;
import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.product.entity.ProductStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProductListResponse {

    private Long id;
    private String name;
    private Integer basePrice;
    private Integer discountRate;
    private Integer discountPrice;
    private String formattedBasePrice;
    private String formattedDiscountPrice;
    private DeliveryType deliveryType;
    private String deliveryTypeDisplayName;
    private Double averageRating;
    private Integer reviewCount;
    private String thumbnailUrl;
    private Boolean isSoldOut;
    private Boolean isRocketDelivery;

    /**
     * Entity + 썸네일 URL -> Response DTO 변환
     */
    public static ProductListResponse from(Product product, String thumbnailUrl) {
        return ProductListResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .basePrice(product.getBasePrice())
                .discountRate(product.getDiscountRate())
                .discountPrice(product.getDiscountPrice())
                .formattedBasePrice(formatPrice(product.getBasePrice()))
                .formattedDiscountPrice(formatPrice(product.getDiscountPrice()))
                .deliveryType(product.getDeliveryType())
                .deliveryTypeDisplayName(product.getDeliveryType().getDisplayName())
                .averageRating(product.getAverageRating())
                .reviewCount(product.getReviewCount())
                .thumbnailUrl(thumbnailUrl)
                .isSoldOut(product.getStatus() == ProductStatus.SOLD_OUT)
                .isRocketDelivery(product.isRocketDelivery())
                .build();
    }

    private static String formatPrice(Integer price) {
        if (price == null) {
            return null;
        }
        return String.format("%,d원", price);
    }
}
