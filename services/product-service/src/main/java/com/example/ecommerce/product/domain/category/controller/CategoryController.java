package com.example.ecommerce.product.domain.category.controller;

import com.example.ecommerce.common.response.ApiResponse;
import com.example.ecommerce.product.domain.category.dto.response.CategoryResponse;
import com.example.ecommerce.product.domain.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public Mono<ApiResponse<List<CategoryResponse>>> getRootCategories() {
        return categoryService.getRootCategories()
                .collectList()
                .map(ApiResponse::success);
    }

    @GetMapping("/{categoryId}")
    public Mono<ApiResponse<CategoryResponse>> getCategory(@PathVariable Long categoryId) {
        return categoryService.getCategory(categoryId)
                .map(ApiResponse::success);
    }

    @GetMapping("/{categoryId}/children")
    public Mono<ApiResponse<List<CategoryResponse>>> getSubCategories(@PathVariable Long categoryId) {
        return categoryService.getSubCategories(categoryId)
                .collectList()
                .map(ApiResponse::success);
    }

    @GetMapping("/{categoryId}/tree")
    public Mono<ApiResponse<CategoryResponse>> getCategoryWithChildren(@PathVariable Long categoryId) {
        return categoryService.getCategoryWithChildren(categoryId)
                .map(ApiResponse::success);
    }
}
