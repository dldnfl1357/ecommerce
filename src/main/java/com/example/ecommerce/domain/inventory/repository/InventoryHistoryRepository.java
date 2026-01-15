package com.example.ecommerce.domain.inventory.repository;

import com.example.ecommerce.domain.inventory.entity.InventoryChangeType;
import com.example.ecommerce.domain.inventory.entity.InventoryHistory;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface InventoryHistoryRepository extends ReactiveCrudRepository<InventoryHistory, Long> {

    // ========== 기본 조회 ==========

    /**
     * 재고 ID로 이력 조회 (최신순)
     */
    Flux<InventoryHistory> findByInventoryIdOrderByCreatedAtDesc(Long inventoryId);

    /**
     * 상품 옵션 ID로 이력 조회 (최신순)
     */
    Flux<InventoryHistory> findByProductOptionIdOrderByCreatedAtDesc(Long productOptionId);

    /**
     * 변경 유형별 이력 조회
     */
    Flux<InventoryHistory> findByChangeTypeOrderByCreatedAtDesc(InventoryChangeType changeType);

    // ========== 페이징 조회 ==========

    /**
     * 재고 ID로 이력 조회 (페이징)
     */
    @Query("""
            SELECT * FROM inventory_histories
            WHERE inventory_id = :inventoryId
            ORDER BY created_at DESC
            LIMIT :limit OFFSET :offset
            """)
    Flux<InventoryHistory> findByInventoryIdWithPaging(
            @Param("inventoryId") Long inventoryId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 상품 옵션 ID로 이력 조회 (페이징)
     */
    @Query("""
            SELECT * FROM inventory_histories
            WHERE product_option_id = :productOptionId
            ORDER BY created_at DESC
            LIMIT :limit OFFSET :offset
            """)
    Flux<InventoryHistory> findByProductOptionIdWithPaging(
            @Param("productOptionId") Long productOptionId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    // ========== 참조 조회 ==========

    /**
     * 주문 ID로 이력 조회
     */
    @Query("""
            SELECT * FROM inventory_histories
            WHERE reference_id = :referenceId AND reference_type = :referenceType
            ORDER BY created_at DESC
            """)
    Flux<InventoryHistory> findByReference(
            @Param("referenceId") Long referenceId,
            @Param("referenceType") String referenceType
    );

    // ========== 통계 ==========

    /**
     * 재고 이력 수 조회
     */
    Mono<Long> countByInventoryId(Long inventoryId);

    /**
     * 상품 옵션별 이력 수 조회
     */
    Mono<Long> countByProductOptionId(Long productOptionId);

    /**
     * 기간별 변경 유형별 이력 수 조회
     */
    @Query("""
            SELECT COUNT(*) FROM inventory_histories
            WHERE change_type = :changeType
            AND created_at >= :startDate
            AND created_at < :endDate
            """)
    Mono<Long> countByChangeTypeAndDateRange(
            @Param("changeType") String changeType,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );
}
