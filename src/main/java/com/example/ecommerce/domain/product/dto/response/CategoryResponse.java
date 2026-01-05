package com.example.ecommerce.domain.product.dto.response;

import com.example.ecommerce.domain.product.entity.Category;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CategoryResponse {

    private Long id;
    private String name;
    private Long parentId;
    private Integer depth;
    private Integer sortOrder;
    private Boolean isActive;
    private String depthLabel;

    /**
     * Entity -> Response DTO 변환
     */
    public static CategoryResponse from(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .parentId(category.getParentId())
                .depth(category.getDepth())
                .sortOrder(category.getSortOrder())
                .isActive(category.getIsActive())
                .depthLabel(getDepthLabel(category.getDepth()))
                .build();
    }

    private static String getDepthLabel(Integer depth) {
        return switch (depth) {
            case 0 -> "대분류";
            case 1 -> "중분류";
            case 2 -> "소분류";
            default -> "기타";
        };
    }
}
