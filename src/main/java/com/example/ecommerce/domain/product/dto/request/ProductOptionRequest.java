package com.example.ecommerce.domain.product.dto.request;

import com.example.ecommerce.domain.product.entity.ProductOption;
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
public class ProductOptionRequest {

    @NotBlank(message = "옵션명은 필수입니다")
    @Size(max = 100, message = "옵션명은 100자 이하여야 합니다")
    private String optionName;

    @Size(max = 50, message = "옵션1은 50자 이하여야 합니다")
    private String option1;

    @Size(max = 50, message = "옵션2는 50자 이하여야 합니다")
    private String option2;

    @Min(value = 0, message = "추가 금액은 0 이상이어야 합니다")
    private Integer addPrice;

    @Min(value = 0, message = "재고 수량은 0 이상이어야 합니다")
    private Integer stockQuantity;

    /**
     * Entity로 변환
     */
    public ProductOption toEntity(Long productId) {
        return ProductOption.builder()
                .productId(productId)
                .optionName(this.optionName)
                .option1(this.option1)
                .option2(this.option2)
                .addPrice(this.addPrice != null ? this.addPrice : 0)
                .stockQuantity(this.stockQuantity != null ? this.stockQuantity : 0)
                .build();
    }
}
