package com.example.ecommerce.product.domain.product.repository;

import com.example.ecommerce.product.domain.product.entity.Product;
import com.example.ecommerce.product.domain.product.entity.ProductStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ProductRepository extends ReactiveCrudRepository<Product, Long> {

    Flux<Product> findBySellerId(Long sellerId);

    Flux<Product> findByCategoryId(Long categoryId);

    Flux<Product> findByStatus(ProductStatus status);

    @Query("SELECT * FROM products WHERE status = :status ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<Product> findByStatusWithPaging(@Param("status") ProductStatus status,
                                          @Param("limit") int limit,
                                          @Param("offset") int offset);

    @Query("SELECT * FROM products WHERE name LIKE CONCAT('%', :keyword, '%') AND status = 'ACTIVE'")
    Flux<Product> searchByName(@Param("keyword") String keyword);

    @Query("SELECT COUNT(*) FROM products WHERE status = :status")
    Mono<Long> countByStatus(@Param("status") ProductStatus status);

    @Query("SELECT * FROM products WHERE category_id = :categoryId AND status = 'ACTIVE' ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<Product> findByCategoryWithPaging(@Param("categoryId") Long categoryId,
                                            @Param("limit") int limit,
                                            @Param("offset") int offset);
}
