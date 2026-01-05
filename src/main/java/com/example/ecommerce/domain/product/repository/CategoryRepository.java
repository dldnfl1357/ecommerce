package com.example.ecommerce.domain.product.repository;

import com.example.ecommerce.domain.product.entity.Category;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CategoryRepository extends ReactiveCrudRepository<Category, Long> {

    /**
     * 최상위 카테고리 조회 (대분류)
     */
    Flux<Category> findByParentIdIsNullAndIsActiveTrue();

    /**
     * 특정 부모의 하위 카테고리 조회
     */
    Flux<Category> findByParentIdAndIsActiveTrue(Long parentId);

    /**
     * 깊이별 카테고리 조회
     */
    Flux<Category> findByDepthAndIsActiveTrue(Integer depth);

    /**
     * 같은 부모 아래 동일 이름 존재 여부
     */
    Mono<Boolean> existsByNameAndParentId(String name, Long parentId);

    /**
     * 활성 카테고리 전체 조회 (정렬)
     */
    @Query("""
            SELECT * FROM categories
            WHERE is_active = true
            ORDER BY depth, sort_order, name
            """)
    Flux<Category> findAllActiveOrderByDepthAndSortOrder();

    /**
     * 하위 카테고리 조회 (정렬)
     */
    @Query("""
            SELECT * FROM categories
            WHERE parent_id = :parentId AND is_active = true
            ORDER BY sort_order, name
            """)
    Flux<Category> findChildrenByParentId(@Param("parentId") Long parentId);

    /**
     * 특정 카테고리의 모든 하위 카테고리 ID 조회 (재귀적)
     */
    @Query("""
            WITH RECURSIVE category_tree AS (
                SELECT id FROM categories WHERE id = :categoryId
                UNION ALL
                SELECT c.id FROM categories c
                INNER JOIN category_tree ct ON c.parent_id = ct.id
                WHERE c.is_active = true
            )
            SELECT id FROM category_tree
            """)
    Flux<Long> findAllDescendantIds(@Param("categoryId") Long categoryId);

    /**
     * 활성 상태인 카테고리 조회
     */
    @Query("SELECT * FROM categories WHERE id = :id AND is_active = true")
    Mono<Category> findByIdAndIsActiveTrue(@Param("id") Long id);
}
