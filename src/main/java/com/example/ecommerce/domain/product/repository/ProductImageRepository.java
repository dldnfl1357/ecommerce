package com.example.ecommerce.domain.product.repository;

import com.example.ecommerce.domain.product.entity.ImageType;
import com.example.ecommerce.domain.product.entity.ProductImage;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ProductImageRepository extends ReactiveCrudRepository<ProductImage, Long> {

    /**
     * 상품별 이미지 조회 (정렬순)
     */
    Flux<ProductImage> findByProductIdOrderBySortOrder(Long productId);

    /**
     * 상품의 썸네일 이미지 조회
     */
    Mono<ProductImage> findByProductIdAndIsThumbnailTrue(Long productId);

    /**
     * 상품별 특정 타입 이미지 조회
     */
    Flux<ProductImage> findByProductIdAndImageType(Long productId, ImageType imageType);

    /**
     * 상품별 이미지 삭제
     */
    Mono<Void> deleteByProductId(Long productId);

    /**
     * 이미지 정렬 (썸네일 우선, 정렬순)
     */
    @Query("""
            SELECT * FROM product_images
            WHERE product_id = :productId
            ORDER BY is_thumbnail DESC, sort_order ASC
            """)
    Flux<ProductImage> findByProductIdOrderByThumbnailAndSort(@Param("productId") Long productId);

    /**
     * 상품 이미지 존재 여부
     */
    Mono<Boolean> existsByProductId(Long productId);

    /**
     * 상품의 첫 번째 이미지 조회 (썸네일 또는 첫 번째)
     */
    @Query("""
            SELECT * FROM product_images
            WHERE product_id = :productId
            ORDER BY is_thumbnail DESC, sort_order ASC
            LIMIT 1
            """)
    Mono<ProductImage> findFirstByProductId(@Param("productId") Long productId);
}
