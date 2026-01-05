package com.example.ecommerce.domain.product.entity;

import com.example.ecommerce.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("product_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage extends BaseEntity {

    @Id
    private Long id;

    @Column("product_id")
    private Long productId;

    @Column("image_url")
    private String imageUrl;

    @Column("sort_order")
    private Integer sortOrder;

    @Column("is_thumbnail")
    private Boolean isThumbnail;

    @Column("image_type")
    private ImageType imageType;

    @Builder
    public ProductImage(Long productId, String imageUrl, Integer sortOrder,
                        Boolean isThumbnail, ImageType imageType) {
        this.productId = productId;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
        this.isThumbnail = isThumbnail != null ? isThumbnail : false;
        this.imageType = imageType != null ? imageType : ImageType.MAIN;
    }

    // ========== 상태 변경 메서드 ==========

    /**
     * 썸네일로 설정
     */
    public ProductImage setAsThumbnail() {
        this.isThumbnail = true;
        return this;
    }

    /**
     * 썸네일 해제
     */
    public ProductImage unsetThumbnail() {
        this.isThumbnail = false;
        return this;
    }

    // ========== 정보 수정 메서드 ==========

    /**
     * 정렬 순서 변경
     */
    public ProductImage updateSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    /**
     * 이미지 URL 변경
     */
    public ProductImage updateImageUrl(String imageUrl) {
        if (imageUrl != null && !imageUrl.isBlank()) {
            this.imageUrl = imageUrl;
        }
        return this;
    }

    /**
     * 이미지 타입 변경
     */
    public ProductImage updateImageType(ImageType imageType) {
        if (imageType != null) {
            this.imageType = imageType;
        }
        return this;
    }

    // ========== 조회 메서드 ==========

    /**
     * 썸네일 여부
     */
    public boolean isThumbnailImage() {
        return Boolean.TRUE.equals(this.isThumbnail);
    }

    /**
     * 메인 이미지 여부
     */
    public boolean isMainImage() {
        return this.imageType == ImageType.MAIN;
    }

    /**
     * 상세 이미지 여부
     */
    public boolean isDetailImage() {
        return this.imageType == ImageType.DETAIL;
    }
}
