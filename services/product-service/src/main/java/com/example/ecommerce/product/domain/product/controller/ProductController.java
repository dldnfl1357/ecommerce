package com.example.ecommerce.product.domain.product.controller;

import com.example.ecommerce.common.response.ApiResponse;
import com.example.ecommerce.product.domain.product.dto.request.ProductCreateRequest;
import com.example.ecommerce.product.domain.product.dto.response.ProductResponse;
import com.example.ecommerce.product.domain.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<ProductResponse>> createProduct(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @Valid @RequestBody ProductCreateRequest request
    ) {
        log.info("상품 생성 요청: sellerId={}", sellerId);
        return productService.createProduct(sellerId, request)
                .map(response -> ApiResponse.success(response, "상품이 등록되었습니다."));
    }

    @GetMapping("/{productId}")
    public Mono<ApiResponse<ProductResponse>> getProduct(@PathVariable Long productId) {
        return productService.getProduct(productId)
                .map(ApiResponse::success);
    }

    @GetMapping("/category/{categoryId}")
    public Mono<ApiResponse<List<ProductResponse>>> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return productService.getProductsByCategory(categoryId, page, size)
                .collectList()
                .map(ApiResponse::success);
    }

    @GetMapping("/search")
    public Mono<ApiResponse<List<ProductResponse>>> searchProducts(
            @RequestParam String keyword
    ) {
        return productService.searchProducts(keyword)
                .collectList()
                .map(ApiResponse::success);
    }

    @PutMapping("/{productId}/publish")
    public Mono<ApiResponse<ProductResponse>> publishProduct(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @PathVariable Long productId
    ) {
        log.info("상품 발행 요청: sellerId={}, productId={}", sellerId, productId);
        return productService.publishProduct(productId, sellerId)
                .map(response -> ApiResponse.success(response, "상품이 발행되었습니다."));
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteProduct(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @PathVariable Long productId
    ) {
        log.info("상품 삭제 요청: sellerId={}, productId={}", sellerId, productId);
        return productService.deleteProduct(productId, sellerId);
    }
}
