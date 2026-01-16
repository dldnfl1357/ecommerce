package com.example.ecommerce.product.domain.category.dto.response;

import com.example.ecommerce.product.domain.category.entity.Category;
import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CategoryResponse {

    private Long id;
    private String name;
    private Long parentId;
    private Integer depth;
    private Integer displayOrder;
    private Boolean isActive;
    private List<CategoryResponse> children;

    public static CategoryResponse from(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .parentId(category.getParentId())
                .depth(category.getDepth())
                .displayOrder(category.getDisplayOrder())
                .isActive(category.getIsActive())
                .build();
    }

    public static CategoryResponse from(Category category, List<CategoryResponse> children) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .parentId(category.getParentId())
                .depth(category.getDepth())
                .displayOrder(category.getDisplayOrder())
                .isActive(category.getIsActive())
                .children(children)
                .build();
    }
}
