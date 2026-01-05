package com.example.ecommerce.domain.product.service;

import com.example.ecommerce.domain.product.dto.response.CategoryResponse;
import com.example.ecommerce.domain.product.dto.response.CategoryTreeResponse;
import com.example.ecommerce.domain.product.entity.Category;
import com.example.ecommerce.domain.product.repository.CategoryRepository;
import com.example.ecommerce.global.exception.BusinessException;
import com.example.ecommerce.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * 전체 카테고리 트리 조회
     */
    public Flux<CategoryTreeResponse> getCategoryTree() {
        return categoryRepository.findByParentIdIsNullAndIsActiveTrue()
                .flatMap(this::buildCategoryTree)
                .doOnComplete(() -> log.info("카테고리 트리 조회 완료"));
    }

    /**
     * 카테고리 상세 조회
     */
    public Mono<CategoryResponse> getCategory(Long categoryId) {
        return categoryRepository.findByIdAndIsActiveTrue(categoryId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.CATEGORY_NOT_FOUND)))
                .map(CategoryResponse::from)
                .doOnSuccess(response -> log.info("카테고리 조회: categoryId={}", categoryId));
    }

    /**
     * 하위 카테고리 조회
     */
    public Flux<CategoryResponse> getChildCategories(Long parentId) {
        return categoryRepository.findChildrenByParentId(parentId)
                .map(CategoryResponse::from)
                .doOnComplete(() -> log.info("하위 카테고리 조회: parentId={}", parentId));
    }

    /**
     * 카테고리 존재 및 활성 여부 검증
     */
    public Mono<Category> validateCategory(Long categoryId) {
        return categoryRepository.findByIdAndIsActiveTrue(categoryId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.CATEGORY_NOT_FOUND)));
    }

    /**
     * 카테고리 및 모든 하위 카테고리 ID 조회
     */
    public Flux<Long> getAllCategoryIds(Long categoryId) {
        return categoryRepository.findAllDescendantIds(categoryId);
    }

    // ========== Private Helper Methods ==========

    /**
     * 카테고리 트리 재귀 생성
     */
    private Mono<CategoryTreeResponse> buildCategoryTree(Category parent) {
        return categoryRepository.findChildrenByParentId(parent.getId())
                .flatMap(this::buildCategoryTree)
                .collectList()
                .map(children -> CategoryTreeResponse.of(parent, children.isEmpty() ? null : children));
    }
}
