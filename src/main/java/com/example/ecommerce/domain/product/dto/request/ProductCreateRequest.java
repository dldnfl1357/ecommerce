package com.example.ecommerce.domain.product.dto.request;

import com.example.ecommerce.domain.product.entity.DeliveryType;
import com.example.ecommerce.domain.product.entity.Product;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProductCreateRequest {

    @NotNull(message = "카테고리 ID는 필수입니다")
    private Long categoryId;

    @NotBlank(message = "상품명은 필수입니다")
    @Size(min = 2, max = 200, message = "상품명은 2자 이상 200자 이하여야 합니다")
    private String name;

    @Size(max = 10000, message = "상품 설명은 10000자 이하여야 합니다")
    private String description;

    @NotNull(message = "기본 가격은 필수입니다")
    @Min(value = 100, message = "가격은 100원 이상이어야 합니다")
    @Max(value = 100_000_000, message = "가격은 1억원 이하여야 합니다")
    private Integer basePrice;

    @Min(value = 0, message = "할인율은 0 이상이어야 합니다")
    @Max(value = 100, message = "할인율은 100 이하여야 합니다")
    private Integer discountRate;

    private DeliveryType deliveryType;

    @Min(value = 0, message = "배송비는 0 이상이어야 합니다")
    private Integer deliveryFee;

    @Min(value = 0, message = "무료배송 기준금액은 0 이상이어야 합니다")
    private Integer freeDeliveryThreshold;

    @Valid
    @NotNull(message = "옵션은 필수입니다")
    @Size(min = 1, message = "최소 1개의 옵션이 필요합니다")
    private List<ProductOptionRequest> options;

    @Valid
    @NotNull(message = "이미지는 필수입니다")
    @Size(min = 1, message = "최소 1개의 이미지가 필요합니다")
    private List<ProductImageRequest> images;

    /**
     * Entity로 변환
     */
    public Product toEntity(Long sellerId) {
        return Product.builder()
                .sellerId(sellerId)
                .categoryId(this.categoryId)
                .name(this.name)
                .description(this.description)
                .basePrice(this.basePrice)
                .discountRate(this.discountRate)
                .deliveryType(this.deliveryType)
                .deliveryFee(this.deliveryFee)
                .freeDeliveryThreshold(this.freeDeliveryThreshold)
                .build();
    }
}
