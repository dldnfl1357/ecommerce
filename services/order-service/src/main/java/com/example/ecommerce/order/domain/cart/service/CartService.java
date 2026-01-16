package com.example.ecommerce.order.domain.cart.service;

import com.example.ecommerce.common.core.exception.BusinessException;
import com.example.ecommerce.common.core.exception.ErrorCode;
import com.example.ecommerce.order.domain.cart.dto.request.CartItemAddRequest;
import com.example.ecommerce.order.domain.cart.dto.request.CartItemUpdateRequest;
import com.example.ecommerce.order.domain.cart.dto.response.CartItemResponse;
import com.example.ecommerce.order.domain.cart.dto.response.CartResponse;
import com.example.ecommerce.order.domain.cart.entity.Cart;
import com.example.ecommerce.order.domain.cart.entity.CartItem;
import com.example.ecommerce.order.domain.cart.repository.CartItemRepository;
import com.example.ecommerce.order.domain.cart.repository.CartRepository;
import com.example.ecommerce.order.external.ProductServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductServiceClient productServiceClient;

    private static final int MAX_CART_ITEMS = 100;

    public Mono<CartResponse> getCart(Long memberId) {
        return getOrCreateCart(memberId)
                .flatMap(cart -> cartItemRepository.findByCartId(cart.getId())
                        .map(CartItemResponse::from)
                        .collectList()
                        .map(items -> CartResponse.of(cart.getId(), memberId, items)));
    }

    @Transactional
    public Mono<CartResponse> addItem(Long memberId, CartItemAddRequest request) {
        return getOrCreateCart(memberId)
                .flatMap(cart -> cartItemRepository.countByCartId(cart.getId())
                        .flatMap(count -> {
                            if (count >= MAX_CART_ITEMS) {
                                return Mono.error(new BusinessException(ErrorCode.CART_ITEM_LIMIT_EXCEEDED));
                            }
                            return cartItemRepository.findByCartIdAndProductOptionId(
                                    cart.getId(), request.getProductOptionId()
                            );
                        })
                        .flatMap(existingItem -> {
                            CartItem updated = existingItem.updateQuantity(
                                    existingItem.getQuantity() + request.getQuantity()
                            );
                            return cartItemRepository.save(updated);
                        })
                        .switchIfEmpty(
                                productServiceClient.getProduct(request.getProductId())
                                        .flatMap(productData -> {
                                            CartItem newItem = CartItem.create(
                                                    cart.getId(),
                                                    request.getProductId(),
                                                    request.getProductOptionId(),
                                                    (String) productData.getOrDefault("name", "상품명"),
                                                    "옵션명",
                                                    request.getQuantity(),
                                                    new BigDecimal(productData.getOrDefault("basePrice", "0").toString()),
                                                    (Integer) productData.getOrDefault("discountRate", 0),
                                                    (Long) productData.get("sellerId")
                                            );
                                            return cartItemRepository.save(newItem);
                                        })
                        )
                        .then(getCart(memberId))
                )
                .doOnSuccess(response -> log.info("장바구니 상품 추가: memberId={}, productOptionId={}",
                        memberId, request.getProductOptionId()));
    }

    @Transactional
    public Mono<CartResponse> updateItemQuantity(Long memberId, Long cartItemId, CartItemUpdateRequest request) {
        return getOrCreateCart(memberId)
                .flatMap(cart -> cartItemRepository.findById(cartItemId)
                        .filter(item -> item.getCartId().equals(cart.getId()))
                        .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND)))
                        .map(item -> item.updateQuantity(request.getQuantity()))
                        .flatMap(cartItemRepository::save)
                )
                .then(getCart(memberId))
                .doOnSuccess(response -> log.info("장바구니 수량 변경: memberId={}, cartItemId={}, quantity={}",
                        memberId, cartItemId, request.getQuantity()));
    }

    @Transactional
    public Mono<CartResponse> removeItem(Long memberId, Long cartItemId) {
        return getOrCreateCart(memberId)
                .flatMap(cart -> cartItemRepository.findById(cartItemId)
                        .filter(item -> item.getCartId().equals(cart.getId()))
                        .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND)))
                        .flatMap(item -> cartItemRepository.delete(item).thenReturn(item))
                )
                .then(getCart(memberId))
                .doOnSuccess(response -> log.info("장바구니 상품 삭제: memberId={}, cartItemId={}",
                        memberId, cartItemId));
    }

    @Transactional
    public Mono<CartResponse> selectItem(Long memberId, Long cartItemId) {
        return getOrCreateCart(memberId)
                .flatMap(cart -> cartItemRepository.findById(cartItemId)
                        .filter(item -> item.getCartId().equals(cart.getId()))
                        .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND)))
                        .map(CartItem::select)
                        .flatMap(cartItemRepository::save)
                )
                .then(getCart(memberId));
    }

    @Transactional
    public Mono<CartResponse> deselectItem(Long memberId, Long cartItemId) {
        return getOrCreateCart(memberId)
                .flatMap(cart -> cartItemRepository.findById(cartItemId)
                        .filter(item -> item.getCartId().equals(cart.getId()))
                        .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND)))
                        .map(CartItem::deselect)
                        .flatMap(cartItemRepository::save)
                )
                .then(getCart(memberId));
    }

    @Transactional
    public Mono<CartResponse> selectAllItems(Long memberId) {
        return getOrCreateCart(memberId)
                .flatMap(cart -> cartItemRepository.findByCartId(cart.getId())
                        .map(CartItem::select)
                        .flatMap(cartItemRepository::save)
                        .then()
                )
                .then(getCart(memberId));
    }

    @Transactional
    public Mono<CartResponse> deselectAllItems(Long memberId) {
        return getOrCreateCart(memberId)
                .flatMap(cart -> cartItemRepository.findByCartId(cart.getId())
                        .map(CartItem::deselect)
                        .flatMap(cartItemRepository::save)
                        .then()
                )
                .then(getCart(memberId));
    }

    @Transactional
    public Mono<Void> clearCart(Long memberId) {
        return getOrCreateCart(memberId)
                .flatMap(cart -> cartItemRepository.deleteByCartId(cart.getId()))
                .doOnSuccess(v -> log.info("장바구니 비우기: memberId={}", memberId));
    }

    @Transactional
    public Mono<Void> removeSelectedItems(Long memberId) {
        return getOrCreateCart(memberId)
                .flatMap(cart -> cartItemRepository.deleteSelectedByCartId(cart.getId()))
                .doOnSuccess(v -> log.info("선택 상품 삭제: memberId={}", memberId));
    }

    public Mono<List<CartItemResponse>> getSelectedItems(Long memberId) {
        return getOrCreateCart(memberId)
                .flatMap(cart -> cartItemRepository.findByCartIdAndIsSelectedTrue(cart.getId())
                        .map(CartItemResponse::from)
                        .collectList());
    }

    private Mono<Cart> getOrCreateCart(Long memberId) {
        return cartRepository.findByMemberId(memberId)
                .switchIfEmpty(
                        cartRepository.save(Cart.create(memberId))
                );
    }
}
