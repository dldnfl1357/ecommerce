package com.example.ecommerce.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/member")
    public Mono<ResponseEntity<Map<String, Object>>> memberFallback() {
        log.warn("Member service is unavailable");
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "result", false,
                        "error", Map.of(
                                "code", "SERVICE_UNAVAILABLE",
                                "message", "회원 서비스가 현재 사용 불가능합니다. 잠시 후 다시 시도해주세요."
                        )
                )));
    }

    @GetMapping("/product")
    public Mono<ResponseEntity<Map<String, Object>>> productFallback() {
        log.warn("Product service is unavailable");
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "result", false,
                        "error", Map.of(
                                "code", "SERVICE_UNAVAILABLE",
                                "message", "상품 서비스가 현재 사용 불가능합니다. 잠시 후 다시 시도해주세요."
                        )
                )));
    }

    @GetMapping("/order")
    public Mono<ResponseEntity<Map<String, Object>>> orderFallback() {
        log.warn("Order service is unavailable");
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "result", false,
                        "error", Map.of(
                                "code", "SERVICE_UNAVAILABLE",
                                "message", "주문 서비스가 현재 사용 불가능합니다. 잠시 후 다시 시도해주세요."
                        )
                )));
    }
}
