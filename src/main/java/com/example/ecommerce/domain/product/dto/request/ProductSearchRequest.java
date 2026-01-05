package com.example.ecommerce.domain.product.dto.request;

import com.example.ecommerce.domain.product.entity.DeliveryType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProductSearchRequest {

    private Long categoryId;

    private String keyword;

    @Min(value = 0, message = "최소 가격은 0 이상이어야 합니다")
    private Integer minPrice;

    @Max(value = 100_000_000, message = "최대 가격은 1억원 이하여야 합니다")
    private Integer maxPrice;

    private DeliveryType deliveryType;

    @Builder.Default
    private SortType sortType = SortType.POPULAR;

    @Min(value = 1, message = "페이지는 1 이상이어야 합니다")
    @Builder.Default
    private Integer page = 1;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다")
    @Builder.Default
    private Integer size = 20;

    /**
     * offset 계산
     */
    public int getOffset() {
        return (page - 1) * size;
    }

    /**
     * 정렬 타입
     */
    @Getter
    @RequiredArgsConstructor
    public enum SortType {
        POPULAR("인기순"),
        NEWEST("신상품순"),
        PRICE_ASC("낮은가격순"),
        PRICE_DESC("높은가격순"),
        RATING("평점순"),
        REVIEW("리뷰많은순");

        private final String displayName;
    }
}
