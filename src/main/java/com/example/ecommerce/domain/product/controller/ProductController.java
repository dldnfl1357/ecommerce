package com.example.ecommerce.domain.product.controller;

import com.example.ecommerce.domain.product.dto.request.ProductCreateRequest;
import com.example.ecommerce.domain.product.dto.request.ProductSearchRequest;
import com.example.ecommerce.domain.product.dto.request.ProductStatusUpdateRequest;
import com.example.ecommerce.domain.product.dto.request.ProductUpdateRequest;
import com.example.ecommerce.domain.product.dto.response.ProductDetailResponse;
import com.example.ecommerce.domain.product.dto.response.ProductListResponse;
import com.example.ecommerce.domain.product.dto.response.ProductResponse;
import com.example.ecommerce.domain.product.service.ProductService;
import com.example.ecommerce.global.common.ApiResponse;
import com.example.ecommerce.global.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 상품 목록 조회 (검색/필터링/정렬)
     * GET /api/v1/products?categoryId=1&sortType=POPULAR&page=1&size=20
     */
    @GetMapping
    public Mono<ApiResponse<PageResponse<ProductListResponse>>> getProducts(
            @Valid ProductSearchRequest request
    ) {
        log.info("상품 목록 조회: categoryId={}, keyword={}, sortType={}",
                request.getCategoryId(), request.getKeyword(), request.getSortType());

        return productService.searchProducts(request)
                .map(response -> ApiResponse.success(response, "상품 목록 조회 성공"))
                .doOnError(error -> log.error("상품 목록 조회 실패", error));
    }

    /**
     * 상품 상세 조회
     * GET /api/v1/products/{id}
     */
    @GetMapping("/{id}")
    public Mono<ApiResponse<ProductDetailResponse>> getProduct(@PathVariable Long id) {
        log.info("상품 상세 조회: productId={}", id);

        return productService.getProductDetail(id)
                .map(response -> ApiResponse.success(response, "상품 조회 성공"))
                .doOnSuccess(response -> log.info("상품 상세 조회 성공: productId={}", id))
                .doOnError(error -> log.error("상품 상세 조회 실패: productId={}", id, error));
    }

    /**
     * 상품 등록 (판매자 전용)
     * POST /api/v1/products
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<ProductDetailResponse>> createProduct(
            @RequestAttribute("memberId") Long memberId,
            @Valid @RequestBody ProductCreateRequest request
    ) {
        log.info("상품 등록 요청: memberId={}, productName={}", memberId, request.getName());

        return productService.createProduct(memberId, request)
                .map(response -> ApiResponse.success(response, "상품이 등록되었습니다."))
                .doOnSuccess(response -> log.info("상품 등록 성공: productId={}, memberId={}",
                        response.getData().getId(), memberId))
                .doOnError(error -> log.error("상품 등록 실패: memberId={}", memberId, error));
    }

    /**
     * 상품 수정 (판매자 전용)
     * PUT /api/v1/products/{id}
     */
    @PutMapping("/{id}")
    public Mono<ApiResponse<ProductDetailResponse>> updateProduct(
            @RequestAttribute("memberId") Long memberId,
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        log.info("상품 수정 요청: memberId={}, productId={}", memberId, id);

        return productService.updateProduct(memberId, id, request)
                .map(response -> ApiResponse.success(response, "상품이 수정되었습니다."))
                .doOnSuccess(response -> log.info("상품 수정 성공: productId={}", id))
                .doOnError(error -> log.error("상품 수정 실패: productId={}", id, error));
    }

    /**
     * 상품 상태 변경 (판매자 전용)
     * PATCH /api/v1/products/{id}/status
     */
    @PatchMapping("/{id}/status")
    public Mono<ApiResponse<ProductResponse>> updateProductStatus(
            @RequestAttribute("memberId") Long memberId,
            @PathVariable Long id,
            @Valid @RequestBody ProductStatusUpdateRequest request
    ) {
        log.info("상품 상태 변경 요청: memberId={}, productId={}, status={}",
                memberId, id, request.getStatus());

        return productService.updateProductStatus(memberId, id, request)
                .map(response -> ApiResponse.success(response, "상품 상태가 변경되었습니다."))
                .doOnSuccess(response -> log.info("상품 상태 변경 성공: productId={}, status={}",
                        id, request.getStatus()))
                .doOnError(error -> log.error("상품 상태 변경 실패: productId={}", id, error));
    }

    /**
     * 상품 삭제 (판매자 전용)
     * DELETE /api/v1/products/{id}
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteProduct(
            @RequestAttribute("memberId") Long memberId,
            @PathVariable Long id
    ) {
        log.info("상품 삭제 요청: memberId={}, productId={}", memberId, id);

        return productService.deleteProduct(memberId, id)
                .doOnSuccess(v -> log.info("상품 삭제 성공: productId={}", id))
                .doOnError(error -> log.error("상품 삭제 실패: productId={}", id, error));
    }
}
