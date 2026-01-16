package com.example.ecommerce.product.domain.category.entity;

import com.example.ecommerce.common.entity.BaseEntity;
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

    @Column("display_order")
    private Integer displayOrder;

    @Column("is_active")
    private Boolean isActive;

    @Builder
    public Category(String name, Long parentId, Integer depth, Integer displayOrder) {
        this.name = name;
        this.parentId = parentId;
        this.depth = depth != null ? depth : 0;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.isActive = true;
    }

    public Category activate() {
        this.isActive = true;
        return this;
    }

    public Category deactivate() {
        this.isActive = false;
        return this;
    }

    public boolean isRootCategory() {
        return this.parentId == null;
    }
}
