package com.example.ecommerce.product.domain.inventory.repository;

import com.example.ecommerce.product.domain.inventory.entity.Inventory;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface InventoryRepository extends ReactiveCrudRepository<Inventory, Long> {

    Mono<Inventory> findByProductOptionId(Long productOptionId);

    Mono<Boolean> existsByProductOptionId(Long productOptionId);

    @Query("SELECT * FROM inventory WHERE quantity - reserved_quantity <= low_stock_threshold")
    Flux<Inventory> findLowStockInventory();

    @Query("SELECT * FROM inventory WHERE quantity - reserved_quantity <= 0")
    Flux<Inventory> findSoldOutInventory();

    @Query("SELECT * FROM inventory WHERE product_option_id IN (:optionIds)")
    Flux<Inventory> findByProductOptionIdIn(@Param("optionIds") Iterable<Long> optionIds);
}
