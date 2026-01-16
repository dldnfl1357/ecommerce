package com.example.ecommerce.product.domain.product.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("product_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage {

    @Id
    private Long id;

    @Column("product_id")
    private Long productId;

    @Column("image_url")
    private String imageUrl;

    @Column("display_order")
    private Integer displayOrder;

    @Column("is_main")
    private Boolean isMain;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @Builder
    public ProductImage(Long productId, String imageUrl, Integer displayOrder, Boolean isMain) {
        this.productId = productId;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.isMain = isMain != null ? isMain : false;
    }
}
