package com.example.ecommerce.product.domain.product.dto.response;

import com.example.ecommerce.product.domain.product.entity.Product;
import com.example.ecommerce.product.domain.product.entity.ProductStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private Long sellerId;
    private Long categoryId;
    private String name;
    private String description;
    private BigDecimal basePrice;
    private BigDecimal sellingPrice;
    private Integer discountRate;
    private ProductStatus status;
    private Boolean isRocketDelivery;
    private Boolean isRocketWow;
    private Long viewCount;
    private Integer reviewCount;
    private BigDecimal ratingAvg;

    private List<ProductOptionResponse> options;
    private List<ProductImageResponse> images;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public static ProductResponse from(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .sellerId(product.getSellerId())
                .categoryId(product.getCategoryId())
                .name(product.getName())
                .description(product.getDescription())
                .basePrice(product.getBasePrice())
                .sellingPrice(product.getSellingPrice())
                .discountRate(product.getDiscountRate())
                .status(product.getStatus())
                .isRocketDelivery(product.getIsRocketDelivery())
                .isRocketWow(product.getIsRocketWow())
                .viewCount(product.getViewCount())
                .reviewCount(product.getReviewCount())
                .ratingAvg(product.getRatingAvg())
                .createdAt(product.getCreatedAt())
                .build();
    }

    public static ProductResponse from(Product product, List<ProductOptionResponse> options, List<ProductImageResponse> images) {
        return ProductResponse.builder()
                .id(product.getId())
                .sellerId(product.getSellerId())
                .categoryId(product.getCategoryId())
                .name(product.getName())
                .description(product.getDescription())
                .basePrice(product.getBasePrice())
                .sellingPrice(product.getSellingPrice())
                .discountRate(product.getDiscountRate())
                .status(product.getStatus())
                .isRocketDelivery(product.getIsRocketDelivery())
                .isRocketWow(product.getIsRocketWow())
                .viewCount(product.getViewCount())
                .reviewCount(product.getReviewCount())
                .ratingAvg(product.getRatingAvg())
                .options(options)
                .images(images)
                .createdAt(product.getCreatedAt())
                .build();
    }
}
