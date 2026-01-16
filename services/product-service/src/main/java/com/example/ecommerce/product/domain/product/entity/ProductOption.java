package com.example.ecommerce.product.domain.product.entity;

import com.example.ecommerce.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("product_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductOption extends BaseEntity {

    @Id
    private Long id;

    @Column("product_id")
    private Long productId;

    @Column("name")
    private String name;

    @Column("additional_price")
    private BigDecimal additionalPrice;

    @Column("is_active")
    private Boolean isActive;

    @Builder
    public ProductOption(Long productId, String name, BigDecimal additionalPrice) {
        this.productId = productId;
        this.name = name;
        this.additionalPrice = additionalPrice != null ? additionalPrice : BigDecimal.ZERO;
        this.isActive = true;
    }

    public ProductOption deactivate() {
        this.isActive = false;
        return this;
    }

    public ProductOption activate() {
        this.isActive = true;
        return this;
    }
}
