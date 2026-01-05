package com.example.ecommerce.domain.product.dto.response;

import com.example.ecommerce.domain.product.entity.Category;
import com.example.ecommerce.domain.product.entity.DeliveryType;
import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.product.entity.ProductImage;
import com.example.ecommerce.domain.product.entity.ProductOption;
import com.example.ecommerce.domain.product.entity.ProductStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProductDetailResponse {

    private Long id;
    private Long sellerId;
    private CategoryResponse category;
    private String name;
    private String description;
    private Integer basePrice;
    private Integer discountRate;
    private Integer discountPrice;
    private String formattedBasePrice;
    private String formattedDiscountPrice;
    private ProductStatus status;
    private String statusDisplayName;
    private DeliveryType deliveryType;
    private String deliveryTypeDisplayName;
    private Integer deliveryFee;
    private Integer freeDeliveryThreshold;
    private String formattedDeliveryFee;
    private String formattedFreeDeliveryThreshold;
    private Double averageRating;
    private Integer reviewCount;
    private Integer salesCount;
    private Integer viewCount;
    private List<ProductOptionResponse> options;
    private List<ProductImageResponse> images;
    private String thumbnailUrl;
    private Boolean isAvailable;
    private Boolean isRocketDelivery;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 복합 생성 메서드 (여러 데이터 조합)
     */
    public static ProductDetailResponse of(Product product,
                                           Category category,
                                           List<ProductOption> options,
                                           List<ProductImage> images) {
        String thumbnail = findThumbnailUrl(images);

        return ProductDetailResponse.builder()
                .id(product.getId())
                .sellerId(product.getSellerId())
                .category(CategoryResponse.from(category))
                .name(product.getName())
                .description(product.getDescription())
                .basePrice(product.getBasePrice())
                .discountRate(product.getDiscountRate())
                .discountPrice(product.getDiscountPrice())
                .formattedBasePrice(formatPrice(product.getBasePrice()))
                .formattedDiscountPrice(formatPrice(product.getDiscountPrice()))
                .status(product.getStatus())
                .statusDisplayName(product.getStatus().getDisplayName())
                .deliveryType(product.getDeliveryType())
                .deliveryTypeDisplayName(product.getDeliveryType().getDisplayName())
                .deliveryFee(product.getDeliveryFee())
                .freeDeliveryThreshold(product.getFreeDeliveryThreshold())
                .formattedDeliveryFee(formatDeliveryFee(product.getDeliveryFee()))
                .formattedFreeDeliveryThreshold(formatPrice(product.getFreeDeliveryThreshold()))
                .averageRating(product.getAverageRating())
                .reviewCount(product.getReviewCount())
                .salesCount(product.getSalesCount())
                .viewCount(product.getViewCount())
                .options(options.stream().map(ProductOptionResponse::from).toList())
                .images(images.stream().map(ProductImageResponse::from).toList())
                .thumbnailUrl(thumbnail)
                .isAvailable(product.isAvailable())
                .isRocketDelivery(product.isRocketDelivery())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private static String findThumbnailUrl(List<ProductImage> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.stream()
                .filter(ProductImage::isThumbnailImage)
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElse(images.get(0).getImageUrl());
    }

    private static String formatPrice(Integer price) {
        if (price == null) {
            return null;
        }
        return String.format("%,d원", price);
    }

    private static String formatDeliveryFee(Integer fee) {
        if (fee == null || fee == 0) {
            return "무료";
        }
        return String.format("%,d원", fee);
    }
}
