package com.example.ecommerce.domain.inventory.repository;

import com.example.ecommerce.domain.inventory.entity.Inventory;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface InventoryRepository extends ReactiveCrudRepository<Inventory, Long> {

    // ========== 기본 조회 ==========

    /**
     * 상품 옵션별 재고 조회
     */
    Mono<Inventory> findByProductOptionId(Long productOptionId);

    /**
     * 여러 상품 옵션의 재고 조회
     */
    Flux<Inventory> findByProductOptionIdIn(Iterable<Long> productOptionIds);

    // ========== 비관적 락 조회 (동시성 제어) ==========

    /**
     * 재고 조회 with 비관적 락 (FOR UPDATE)
     */
    @Query("SELECT * FROM inventories WHERE id = :id FOR UPDATE")
    Mono<Inventory> findByIdWithLock(@Param("id") Long id);

    /**
     * 상품 옵션 ID로 재고 조회 with 비관적 락
     */
    @Query("SELECT * FROM inventories WHERE product_option_id = :productOptionId FOR UPDATE")
    Mono<Inventory> findByProductOptionIdWithLock(@Param("productOptionId") Long productOptionId);

    // ========== 재고 상태 조회 ==========

    /**
     * 품절 상품 옵션 조회
     */
    @Query("""
            SELECT * FROM inventories
            WHERE quantity - reserved_quantity <= 0
            """)
    Flux<Inventory> findSoldOutInventories();

    /**
     * 안전 재고 이하 상품 옵션 조회
     */
    @Query("""
            SELECT * FROM inventories
            WHERE quantity - reserved_quantity <= safety_stock
            AND quantity - reserved_quantity > 0
            """)
    Flux<Inventory> findLowStockInventories();

    /**
     * 특정 상품의 모든 옵션 재고 합계
     */
    @Query("""
            SELECT COALESCE(SUM(i.quantity - i.reserved_quantity), 0)
            FROM inventories i
            JOIN product_options po ON i.product_option_id = po.id
            WHERE po.product_id = :productId
            """)
    Mono<Integer> getTotalAvailableQuantityByProductId(@Param("productId") Long productId);

    /**
     * 특정 상품의 모든 옵션 재고 조회
     */
    @Query("""
            SELECT i.* FROM inventories i
            JOIN product_options po ON i.product_option_id = po.id
            WHERE po.product_id = :productId
            """)
    Flux<Inventory> findByProductId(@Param("productId") Long productId);

    // ========== 재고 수량 업데이트 (Atomic) ==========

    /**
     * 재고 증가 (원자적 연산)
     */
    @Query("""
            UPDATE inventories
            SET quantity = quantity + :amount,
                updated_at = CURRENT_TIMESTAMP,
                version = version + 1
            WHERE id = :id
            """)
    Mono<Integer> increaseQuantity(@Param("id") Long id, @Param("amount") int amount);

    /**
     * 재고 감소 (원자적 연산, 재고 부족 시 실패)
     */
    @Query("""
            UPDATE inventories
            SET quantity = quantity - :amount,
                updated_at = CURRENT_TIMESTAMP,
                version = version + 1
            WHERE id = :id
            AND quantity >= :amount
            """)
    Mono<Integer> decreaseQuantity(@Param("id") Long id, @Param("amount") int amount);

    /**
     * 예약 수량 증가 (원자적 연산, 가용 재고 부족 시 실패)
     */
    @Query("""
            UPDATE inventories
            SET reserved_quantity = reserved_quantity + :amount,
                updated_at = CURRENT_TIMESTAMP,
                version = version + 1
            WHERE id = :id
            AND quantity - reserved_quantity >= :amount
            """)
    Mono<Integer> increaseReservedQuantity(@Param("id") Long id, @Param("amount") int amount);

    /**
     * 예약 수량 감소 (원자적 연산)
     */
    @Query("""
            UPDATE inventories
            SET reserved_quantity = reserved_quantity - :amount,
                updated_at = CURRENT_TIMESTAMP,
                version = version + 1
            WHERE id = :id
            AND reserved_quantity >= :amount
            """)
    Mono<Integer> decreaseReservedQuantity(@Param("id") Long id, @Param("amount") int amount);

    /**
     * 예약 확정 (예약 감소 + 재고 감소를 한번에)
     */
    @Query("""
            UPDATE inventories
            SET quantity = quantity - :amount,
                reserved_quantity = reserved_quantity - :amount,
                updated_at = CURRENT_TIMESTAMP,
                version = version + 1
            WHERE id = :id
            AND quantity >= :amount
            AND reserved_quantity >= :amount
            """)
    Mono<Integer> confirmReservation(@Param("id") Long id, @Param("amount") int amount);

    // ========== 존재 여부 확인 ==========

    /**
     * 상품 옵션에 대한 재고 존재 여부
     */
    Mono<Boolean> existsByProductOptionId(Long productOptionId);
}
