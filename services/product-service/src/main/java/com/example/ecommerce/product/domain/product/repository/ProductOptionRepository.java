package com.example.ecommerce.product.domain.product.repository;

import com.example.ecommerce.product.domain.product.entity.ProductOption;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ProductOptionRepository extends ReactiveCrudRepository<ProductOption, Long> {

    Flux<ProductOption> findByProductId(Long productId);

    Flux<ProductOption> findByProductIdAndIsActiveTrue(Long productId);

    Mono<Void> deleteByProductId(Long productId);
}
