package com.example.ecommerce.order.external;

import com.example.ecommerce.common.core.exception.BusinessException;
import com.example.ecommerce.common.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductServiceClient {

    @Qualifier("productServiceClient")
    private final WebClient productServiceClient;

    public Mono<Map<String, Object>> reserveStock(Long productOptionId, Integer quantity, Long orderId) {
        log.info("재고 예약 요청: productOptionId={}, quantity={}", productOptionId, quantity);

        return productServiceClient.post()
                .uri("/internal/api/v1/inventory/reserve")
                .bodyValue(Map.of(
                        "productOptionId", productOptionId,
                        "quantity", quantity,
                        "orderId", orderId
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response.get("data"))
                .doOnSuccess(response -> log.info("재고 예약 성공: productOptionId={}", productOptionId))
                .onErrorMap(WebClientResponseException.class, e -> {
                    log.error("재고 예약 실패: productOptionId={}, status={}", productOptionId, e.getStatusCode());
                    if (e.getStatusCode().value() == 400) {
                        return new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
                    }
                    return new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
                });
    }

    public Mono<Map<String, Object>> releaseStock(Long productOptionId, Integer quantity, Long orderId, String reason) {
        log.info("재고 해제 요청: productOptionId={}, quantity={}", productOptionId, quantity);

        return productServiceClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/api/v1/inventory/option/{productOptionId}/release")
                        .queryParam("quantity", quantity)
                        .queryParam("orderId", orderId)
                        .queryParam("reason", reason)
                        .build(productOptionId))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response.get("data"))
                .doOnSuccess(response -> log.info("재고 해제 성공: productOptionId={}", productOptionId))
                .onErrorMap(WebClientResponseException.class, e -> {
                    log.error("재고 해제 실패: productOptionId={}, status={}", productOptionId, e.getStatusCode());
                    return new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
                });
    }

    public Mono<Map<String, Object>> getProduct(Long productId) {
        return productServiceClient.get()
                .uri("/internal/api/v1/products/{productId}", productId)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response.get("data"))
                .onErrorMap(WebClientResponseException.NotFound.class, e ->
                        new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
