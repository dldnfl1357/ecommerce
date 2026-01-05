package com.example.ecommerce.domain.product.dto.response;

import com.example.ecommerce.domain.product.entity.Category;
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
public class CategoryTreeResponse {

    private Long id;
    private String name;
    private Integer depth;
    private Integer sortOrder;
    private List<CategoryTreeResponse> children;

    /**
     * Entity + 자식 목록 -> Response DTO 변환
     */
    public static CategoryTreeResponse of(Category category, List<CategoryTreeResponse> children) {
        return CategoryTreeResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .depth(category.getDepth())
                .sortOrder(category.getSortOrder())
                .children(children != null && !children.isEmpty() ? children : null)
                .build();
    }

    /**
     * Entity -> Response DTO 변환 (자식 없음)
     */
    public static CategoryTreeResponse from(Category category) {
        return of(category, null);
    }
}
