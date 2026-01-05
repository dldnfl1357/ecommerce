package com.example.ecommerce.domain.product.repository;

import com.example.ecommerce.domain.product.entity.ProductOption;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ProductOptionRepository extends ReactiveCrudRepository<ProductOption, Long> {

    /**
     * 상품별 모든 옵션 조회
     */
    Flux<ProductOption> findByProductId(Long productId);

    /**
     * 상품별 활성 옵션 조회
     */
    Flux<ProductOption> findByProductIdAndIsAvailableTrue(Long productId);

    /**
     * 특정 상품의 특정 옵션 조회
     */
    Mono<ProductOption> findByIdAndProductId(Long id, Long productId);

    /**
     * 상품별 옵션 삭제
     */
    Mono<Void> deleteByProductId(Long productId);

    /**
     * 재고 있는 옵션 조회
     */
    @Query("""
            SELECT * FROM product_options
            WHERE product_id = :productId
            AND is_available = true
            AND stock_quantity > 0
            """)
    Flux<ProductOption> findAvailableByProductId(@Param("productId") Long productId);

    /**
     * 재고 있는 옵션 개수
     */
    @Query("""
            SELECT COUNT(*) FROM product_options
            WHERE product_id = :productId
            AND stock_quantity > 0
            """)
    Mono<Long> countAvailableByProductId(@Param("productId") Long productId);

    /**
     * 상품의 총 재고 수량
     */
    @Query("""
            SELECT COALESCE(SUM(stock_quantity), 0) FROM product_options
            WHERE product_id = :productId
            """)
    Mono<Integer> getTotalStockByProductId(@Param("productId") Long productId);

    /**
     * 상품 옵션 존재 여부
     */
    Mono<Boolean> existsByProductId(Long productId);
}
