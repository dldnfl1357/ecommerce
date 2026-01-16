package com.example.ecommerce.product.domain.product.repository;

import com.example.ecommerce.product.domain.product.entity.ProductImage;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ProductImageRepository extends ReactiveCrudRepository<ProductImage, Long> {

    Flux<ProductImage> findByProductIdOrderByDisplayOrderAsc(Long productId);

    Mono<ProductImage> findByProductIdAndIsMainTrue(Long productId);

    Mono<Void> deleteByProductId(Long productId);
}
