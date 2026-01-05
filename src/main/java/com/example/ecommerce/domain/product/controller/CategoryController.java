package com.example.ecommerce.domain.product.controller;

import com.example.ecommerce.domain.product.dto.response.CategoryResponse;
import com.example.ecommerce.domain.product.dto.response.CategoryTreeResponse;
import com.example.ecommerce.domain.product.service.CategoryService;
import com.example.ecommerce.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 전체 카테고리 트리 조회
     * GET /api/v1/categories
     */
    @GetMapping
    public Mono<ApiResponse<List<CategoryTreeResponse>>> getCategories() {
        log.info("카테고리 트리 조회 요청");

        return categoryService.getCategoryTree()
                .collectList()
                .map(response -> ApiResponse.success(response, "카테고리 조회 성공"))
                .doOnSuccess(response -> log.info("카테고리 트리 조회 성공"))
                .doOnError(error -> log.error("카테고리 트리 조회 실패", error));
    }

    /**
     * 카테고리 상세 조회
     * GET /api/v1/categories/{id}
     */
    @GetMapping("/{id}")
    public Mono<ApiResponse<CategoryResponse>> getCategory(@PathVariable Long id) {
        log.info("카테고리 상세 조회: categoryId={}", id);

        return categoryService.getCategory(id)
                .map(ApiResponse::success)
                .doOnSuccess(response -> log.info("카테고리 상세 조회 성공: categoryId={}", id))
                .doOnError(error -> log.error("카테고리 상세 조회 실패: categoryId={}", id, error));
    }

    /**
     * 하위 카테고리 조회
     * GET /api/v1/categories/{id}/children
     */
    @GetMapping("/{id}/children")
    public Mono<ApiResponse<List<CategoryResponse>>> getChildCategories(@PathVariable Long id) {
        log.info("하위 카테고리 조회: parentId={}", id);

        return categoryService.getChildCategories(id)
                .collectList()
                .map(response -> ApiResponse.success(response, "하위 카테고리 조회 성공"))
                .doOnSuccess(response -> log.info("하위 카테고리 조회 성공: parentId={}", id))
                .doOnError(error -> log.error("하위 카테고리 조회 실패: parentId={}", id, error));
    }
}
