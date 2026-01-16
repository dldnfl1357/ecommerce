package com.example.ecommerce.product.domain.category.repository;

import com.example.ecommerce.product.domain.category.entity.Category;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface CategoryRepository extends ReactiveCrudRepository<Category, Long> {

    Flux<Category> findByParentIdIsNullAndIsActiveTrueOrderByDisplayOrderAsc();

    Flux<Category> findByParentIdAndIsActiveTrueOrderByDisplayOrderAsc(Long parentId);

    Flux<Category> findByDepthAndIsActiveTrueOrderByDisplayOrderAsc(Integer depth);

    Flux<Category> findByIsActiveTrueOrderByDepthAscDisplayOrderAsc();
}
