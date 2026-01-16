package com.example.ecommerce.product.domain.seller.controller;

import com.example.ecommerce.common.response.ApiResponse;
import com.example.ecommerce.product.domain.seller.dto.request.SellerRegisterRequest;
import com.example.ecommerce.product.domain.seller.dto.response.SellerResponse;
import com.example.ecommerce.product.domain.seller.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<SellerResponse>> registerSeller(
            @RequestHeader("X-Member-Id") Long memberId,
            @Valid @RequestBody SellerRegisterRequest request
    ) {
        log.info("판매자 등록 요청: memberId={}", memberId);
        return sellerService.registerSeller(memberId, request)
                .map(response -> ApiResponse.success(response, "판매자 등록 신청이 완료되었습니다."));
    }

    @GetMapping("/{sellerId}")
    public Mono<ApiResponse<SellerResponse>> getSeller(@PathVariable Long sellerId) {
        return sellerService.getSeller(sellerId)
                .map(ApiResponse::success);
    }

    @GetMapping("/me")
    public Mono<ApiResponse<SellerResponse>> getMySeller(
            @RequestHeader("X-Member-Id") Long memberId
    ) {
        return sellerService.getSellerByMemberId(memberId)
                .map(ApiResponse::success);
    }

    // Admin API
    @PutMapping("/{sellerId}/approve")
    public Mono<ApiResponse<SellerResponse>> approveSeller(@PathVariable Long sellerId) {
        log.info("판매자 승인 요청: sellerId={}", sellerId);
        return sellerService.approveSeller(sellerId)
                .map(response -> ApiResponse.success(response, "판매자가 승인되었습니다."));
    }

    @PutMapping("/{sellerId}/suspend")
    public Mono<ApiResponse<SellerResponse>> suspendSeller(@PathVariable Long sellerId) {
        log.info("판매자 정지 요청: sellerId={}", sellerId);
        return sellerService.suspendSeller(sellerId)
                .map(response -> ApiResponse.success(response, "판매자가 정지되었습니다."));
    }
}
