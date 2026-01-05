package com.example.ecommerce.domain.product.dto.response;

import com.example.ecommerce.domain.product.entity.ProductOption;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProductOptionResponse {

    private Long id;
    private String optionName;
    private String option1;
    private String option2;
    private Integer addPrice;
    private String formattedAddPrice;
    private Integer stockQuantity;
    private Boolean isAvailable;
    private Boolean hasStock;

    /**
     * Entity -> Response DTO 변환
     */
    public static ProductOptionResponse from(ProductOption option) {
        return ProductOptionResponse.builder()
                .id(option.getId())
                .optionName(option.getOptionName())
                .option1(option.getOption1())
                .option2(option.getOption2())
                .addPrice(option.getAddPrice())
                .formattedAddPrice(formatAddPrice(option.getAddPrice()))
                .stockQuantity(option.getStockQuantity())
                .isAvailable(option.getIsAvailable())
                .hasStock(option.hasStock())
                .build();
    }

    private static String formatAddPrice(Integer addPrice) {
        if (addPrice == null || addPrice == 0) {
            return null;
        }
        return String.format("+%,d원", addPrice);
    }
}
