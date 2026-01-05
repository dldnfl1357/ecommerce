package com.example.ecommerce.domain.product.repository;

import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.product.entity.ProductStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ProductRepository extends ReactiveCrudRepository<Product, Long> {

    // ========== 기본 조회 ==========

    /**
     * 삭제되지 않은 상품 조회
     */
    Mono<Product> findByIdAndStatusNot(Long id, ProductStatus status);

    /**
     * 판매자별 상품 조회
     */
    Flux<Product> findBySellerId(Long sellerId);

    /**
     * 카테고리별 상품 조회
     */
    Flux<Product> findByCategoryId(Long categoryId);

    /**
     * 상태별 상품 조회
     */
    Flux<Product> findByStatus(ProductStatus status);

    /**
     * 상태별 상품 수 조회
     */
    Mono<Long> countByStatus(ProductStatus status);

    /**
     * 카테고리 및 상태별 상품 수 조회
     */
    Mono<Long> countByCategoryIdAndStatus(Long categoryId, ProductStatus status);

    // ========== 페이징 조회 (카테고리별) ==========

    /**
     * 카테고리별 상품 조회 (최신순 - 기본)
     */
    @Query("""
            SELECT * FROM products
            WHERE category_id = :categoryId AND status = 'ON_SALE'
            ORDER BY created_at DESC
            LIMIT :limit OFFSET :offset
            """)
    Flux<Product> findByCategoryIdOrderByNewest(
            @Param("categoryId") Long categoryId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 카테고리별 상품 조회 (인기순 - 판매량)
     */
    @Query("""
            SELECT * FROM products
            WHERE category_id = :categoryId AND status = 'ON_SALE'
            ORDER BY sales_count DESC, created_at DESC
            LIMIT :limit OFFSET :offset
            """)
    Flux<Product> findByCategoryIdOrderBySalesCount(
            @Param("categoryId") Long categoryId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 카테고리별 상품 조회 (평점순)
     */
    @Query("""
            SELECT * FROM products
            WHERE category_id = :categoryId AND status = 'ON_SALE'
            ORDER BY average_rating DESC, review_count DESC
            LIMIT :limit OFFSET :offset
            """)
    Flux<Product> findByCategoryIdOrderByRating(
            @Param("categoryId") Long categoryId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 카테고리별 상품 조회 (낮은 가격순)
     */
    @Query("""
            SELECT * FROM products
            WHERE category_id = :categoryId AND status = 'ON_SALE'
            ORDER BY discount_price ASC
            LIMIT :limit OFFSET :offset
            """)
    Flux<Product> findByCategoryIdOrderByPriceAsc(
            @Param("categoryId") Long categoryId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 카테고리별 상품 조회 (높은 가격순)
     */
    @Query("""
            SELECT * FROM products
            WHERE category_id = :categoryId AND status = 'ON_SALE'
            ORDER BY discount_price DESC
            LIMIT :limit OFFSET :offset
            """)
    Flux<Product> findByCategoryIdOrderByPriceDesc(
            @Param("categoryId") Long categoryId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 카테고리별 상품 조회 (리뷰 많은순)
     */
    @Query("""
            SELECT * FROM products
            WHERE category_id = :categoryId AND status = 'ON_SALE'
            ORDER BY review_count DESC
            LIMIT :limit OFFSET :offset
            """)
    Flux<Product> findByCategoryIdOrderByReviewCount(
            @Param("categoryId") Long categoryId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    // ========== 필터링 조회 ==========

    /**
     * 가격 범위 필터링
     */
    @Query("""
            SELECT * FROM products
            WHERE category_id = :categoryId AND status = 'ON_SALE'
            AND discount_price BETWEEN :minPrice AND :maxPrice
            ORDER BY discount_price ASC
            LIMIT :limit OFFSET :offset
            """)
    Flux<Product> findByCategoryIdAndPriceRange(
            @Param("categoryId") Long categoryId,
            @Param("minPrice") int minPrice,
            @Param("maxPrice") int maxPrice,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 배송 타입 필터링
     */
    @Query("""
            SELECT * FROM products
            WHERE category_id = :categoryId AND status = 'ON_SALE'
            AND delivery_type = :deliveryType
            ORDER BY created_at DESC
            LIMIT :limit OFFSET :offset
            """)
    Flux<Product> findByCategoryIdAndDeliveryType(
            @Param("categoryId") Long categoryId,
            @Param("deliveryType") String deliveryType,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    // ========== 검색 ==========

    /**
     * 상품명 검색 (인기순)
     */
    @Query("""
            SELECT * FROM products
            WHERE status = 'ON_SALE'
            AND name LIKE CONCAT('%', :keyword, '%')
            ORDER BY sales_count DESC
            LIMIT :limit OFFSET :offset
            """)
    Flux<Product> searchByKeyword(
            @Param("keyword") String keyword,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 검색 결과 개수
     */
    @Query("""
            SELECT COUNT(*) FROM products
            WHERE status = 'ON_SALE'
            AND name LIKE CONCAT('%', :keyword, '%')
            """)
    Mono<Long> countByKeyword(@Param("keyword") String keyword);

    // ========== 판매자 전용 ==========

    /**
     * 판매자의 상품 목록 (페이징)
     */
    @Query("""
            SELECT * FROM products
            WHERE seller_id = :sellerId AND status != 'DELETED'
            ORDER BY created_at DESC
            LIMIT :limit OFFSET :offset
            """)
    Flux<Product> findBySellerIdWithPaging(
            @Param("sellerId") Long sellerId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 판매자의 상품 수
     */
    @Query("""
            SELECT COUNT(*) FROM products
            WHERE seller_id = :sellerId AND status != 'DELETED'
            """)
    Mono<Long> countBySellerId(@Param("sellerId") Long sellerId);
}
