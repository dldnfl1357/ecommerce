package com.example.ecommerce.domain.product.entity;

import com.example.ecommerce.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseEntity {

    @Id
    private Long id;

    @Column("name")
    private String name;

    @Column("parent_id")
    private Long parentId;

    @Column("depth")
    private Integer depth;

    @Column("sort_order")
    private Integer sortOrder;

    @Column("is_active")
    private Boolean isActive;

    @Builder
    public Category(String name, Long parentId, Integer depth, Integer sortOrder) {
        this.name = name;
        this.parentId = parentId;
        this.depth = depth != null ? depth : 0;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
        this.isActive = true;
    }

    // ========== 비즈니스 로직 ==========

    /**
     * 카테고리 활성화
     */
    public Category activate() {
        this.isActive = true;
        return this;
    }

    /**
     * 카테고리 비활성화
     */
    public Category deactivate() {
        this.isActive = false;
        return this;
    }

    /**
     * 카테고리 정보 수정
     */
    public Category updateInfo(String name, Integer sortOrder) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
        return this;
    }

    // ========== 조회 메서드 ==========

    /**
     * 최상위(루트) 카테고리 여부
     */
    public boolean isRoot() {
        return this.parentId == null;
    }

    /**
     * 최하위(리프) 카테고리 여부 (소분류: depth == 2)
     */
    public boolean isLeaf() {
        return this.depth == 2;
    }

    /**
     * 대분류 여부
     */
    public boolean isMainCategory() {
        return this.depth == 0;
    }

    /**
     * 중분류 여부
     */
    public boolean isSubCategory() {
        return this.depth == 1;
    }

    /**
     * 소분류 여부
     */
    public boolean isDetailCategory() {
        return this.depth == 2;
    }

    /**
     * 활성 상태 여부
     */
    public boolean isActiveCategory() {
        return Boolean.TRUE.equals(this.isActive);
    }
}
