package com.example.ecommerce.product.domain.category.service;

import com.example.ecommerce.common.exception.BusinessException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.product.domain.category.dto.response.CategoryResponse;
import com.example.ecommerce.product.domain.category.repository.CategoryRepository;
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

    public Flux<CategoryResponse> getRootCategories() {
        return categoryRepository.findByParentIdIsNullAndIsActiveTrueOrderByDisplayOrderAsc()
                .map(CategoryResponse::from);
    }

    public Mono<CategoryResponse> getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.CATEGORY_NOT_FOUND)))
                .map(CategoryResponse::from);
    }

    public Flux<CategoryResponse> getSubCategories(Long parentId) {
        return categoryRepository.findByParentIdAndIsActiveTrueOrderByDisplayOrderAsc(parentId)
                .map(CategoryResponse::from);
    }

    public Mono<CategoryResponse> getCategoryWithChildren(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.CATEGORY_NOT_FOUND)))
                .flatMap(category ->
                    getSubCategories(category.getId())
                            .collectList()
                            .map(children -> CategoryResponse.from(category, children))
                );
    }

    public Mono<List<CategoryResponse>> getCategoryTree() {
        return categoryRepository.findByParentIdIsNullAndIsActiveTrueOrderByDisplayOrderAsc()
                .flatMap(rootCategory ->
                    getSubCategories(rootCategory.getId())
                            .flatMap(child ->
                                getSubCategories(child.getId())
                                        .collectList()
                                        .map(grandChildren -> CategoryResponse.from(
                                                categoryRepository.findById(child.getId()).block(),
                                                grandChildren
                                        ))
                            )
                            .collectList()
                            .map(children -> CategoryResponse.from(rootCategory, children))
                )
                .collectList();
    }
}
