package com.example.ecommerce.domain.product.dto.response;

import com.example.ecommerce.domain.product.entity.DeliveryType;
import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.product.entity.ProductStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private Long sellerId;
    private Long categoryId;
    private String name;
    private Integer basePrice;
    private Integer discountRate;
    private Integer discountPrice;
    private String formattedBasePrice;
    private String formattedDiscountPrice;
    private ProductStatus status;
    private String statusDisplayName;
    private DeliveryType deliveryType;
    private String deliveryTypeDisplayName;
    private Double averageRating;
    private Integer reviewCount;
    private Integer salesCount;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * Entity -> Response DTO 변환
     */
    public static ProductResponse from(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .sellerId(product.getSellerId())
                .categoryId(product.getCategoryId())
                .name(product.getName())
                .basePrice(product.getBasePrice())
                .discountRate(product.getDiscountRate())
                .discountPrice(product.getDiscountPrice())
                .formattedBasePrice(formatPrice(product.getBasePrice()))
                .formattedDiscountPrice(formatPrice(product.getDiscountPrice()))
                .status(product.getStatus())
                .statusDisplayName(product.getStatus().getDisplayName())
                .deliveryType(product.getDeliveryType())
                .deliveryTypeDisplayName(product.getDeliveryType().getDisplayName())
                .averageRating(product.getAverageRating())
                .reviewCount(product.getReviewCount())
                .salesCount(product.getSalesCount())
                .createdAt(product.getCreatedAt())
                .build();
    }

    private static String formatPrice(Integer price) {
        if (price == null) {
            return null;
        }
        return String.format("%,d원", price);
    }
}
