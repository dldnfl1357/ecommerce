package com.example.ecommerce.order.domain.cart.controller;

import com.example.ecommerce.common.core.response.ApiResponse;
import com.example.ecommerce.order.domain.cart.dto.request.CartItemAddRequest;
import com.example.ecommerce.order.domain.cart.dto.request.CartItemUpdateRequest;
import com.example.ecommerce.order.domain.cart.dto.response.CartItemResponse;
import com.example.ecommerce.order.domain.cart.dto.response.CartResponse;
import com.example.ecommerce.order.domain.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public Mono<ApiResponse<CartResponse>> getCart(
            @RequestAttribute("memberId") Long memberId
    ) {
        return cartService.getCart(memberId)
                .map(ApiResponse::success);
    }

    @PostMapping("/items")
    public Mono<ApiResponse<CartResponse>> addItem(
            @RequestAttribute("memberId") Long memberId,
            @Valid @RequestBody CartItemAddRequest request
    ) {
        log.info("장바구니 추가 요청: memberId={}, productOptionId={}", memberId, request.getProductOptionId());
        return cartService.addItem(memberId, request)
                .map(response -> ApiResponse.success(response, "상품이 장바구니에 추가되었습니다."));
    }

    @PutMapping("/items/{cartItemId}")
    public Mono<ApiResponse<CartResponse>> updateItemQuantity(
            @RequestAttribute("memberId") Long memberId,
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartItemUpdateRequest request
    ) {
        log.info("장바구니 수량 변경: memberId={}, cartItemId={}", memberId, cartItemId);
        return cartService.updateItemQuantity(memberId, cartItemId, request)
                .map(response -> ApiResponse.success(response, "수량이 변경되었습니다."));
    }

    @DeleteMapping("/items/{cartItemId}")
    public Mono<ApiResponse<CartResponse>> removeItem(
            @RequestAttribute("memberId") Long memberId,
            @PathVariable Long cartItemId
    ) {
        log.info("장바구니 삭제: memberId={}, cartItemId={}", memberId, cartItemId);
        return cartService.removeItem(memberId, cartItemId)
                .map(response -> ApiResponse.success(response, "상품이 삭제되었습니다."));
    }

    @PostMapping("/items/{cartItemId}/select")
    public Mono<ApiResponse<CartResponse>> selectItem(
            @RequestAttribute("memberId") Long memberId,
            @PathVariable Long cartItemId
    ) {
        return cartService.selectItem(memberId, cartItemId)
                .map(ApiResponse::success);
    }

    @PostMapping("/items/{cartItemId}/deselect")
    public Mono<ApiResponse<CartResponse>> deselectItem(
            @RequestAttribute("memberId") Long memberId,
            @PathVariable Long cartItemId
    ) {
        return cartService.deselectItem(memberId, cartItemId)
                .map(ApiResponse::success);
    }

    @PostMapping("/select-all")
    public Mono<ApiResponse<CartResponse>> selectAllItems(
            @RequestAttribute("memberId") Long memberId
    ) {
        return cartService.selectAllItems(memberId)
                .map(response -> ApiResponse.success(response, "전체 상품이 선택되었습니다."));
    }

    @PostMapping("/deselect-all")
    public Mono<ApiResponse<CartResponse>> deselectAllItems(
            @RequestAttribute("memberId") Long memberId
    ) {
        return cartService.deselectAllItems(memberId)
                .map(response -> ApiResponse.success(response, "전체 상품 선택이 해제되었습니다."));
    }

    @DeleteMapping
    public Mono<ApiResponse<Void>> clearCart(
            @RequestAttribute("memberId") Long memberId
    ) {
        log.info("장바구니 비우기: memberId={}", memberId);
        return cartService.clearCart(memberId)
                .then(Mono.just(ApiResponse.success(null, "장바구니가 비워졌습니다.")));
    }

    @DeleteMapping("/selected")
    public Mono<ApiResponse<Void>> removeSelectedItems(
            @RequestAttribute("memberId") Long memberId
    ) {
        log.info("선택 상품 삭제: memberId={}", memberId);
        return cartService.removeSelectedItems(memberId)
                .then(Mono.just(ApiResponse.success(null, "선택한 상품이 삭제되었습니다.")));
    }

    @GetMapping("/selected")
    public Mono<ApiResponse<List<CartItemResponse>>> getSelectedItems(
            @RequestAttribute("memberId") Long memberId
    ) {
        return cartService.getSelectedItems(memberId)
                .map(ApiResponse::success);
    }
}
