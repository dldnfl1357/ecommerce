package com.example.ecommerce.domain.seller.controller;

import com.example.ecommerce.domain.seller.dto.request.SellerRegisterRequest;
import com.example.ecommerce.domain.seller.dto.request.SellerUpdateRequest;
import com.example.ecommerce.domain.seller.dto.response.SellerResponse;
import com.example.ecommerce.domain.seller.service.SellerService;
import com.example.ecommerce.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;

    /**
     * 판매자 등록
     * POST /api/v1/sellers
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<SellerResponse>> registerSeller(
            @RequestAttribute("memberId") Long memberId,
            @Valid @RequestBody SellerRegisterRequest request
    ) {
        log.info("판매자 등록 요청: memberId={}, businessName={}", memberId, request.getBusinessName());

        return sellerService.registerSeller(memberId, request)
                .map(response -> ApiResponse.success(response, "판매자 등록이 완료되었습니다."))
                .doOnSuccess(response -> log.info("판매자 등록 성공: memberId={}", memberId))
                .doOnError(error -> log.error("판매자 등록 실패: memberId={}", memberId, error));
    }

    /**
     * 내 판매자 정보 조회
     * GET /api/v1/sellers/me
     */
    @GetMapping("/me")
    public Mono<ApiResponse<SellerResponse>> getMySeller(
            @RequestAttribute("memberId") Long memberId
    ) {
        log.info("내 판매자 정보 조회: memberId={}", memberId);

        return sellerService.getMySeller(memberId)
                .map(ApiResponse::success)
                .doOnSuccess(response -> log.info("판매자 정보 조회 성공: memberId={}", memberId))
                .doOnError(error -> log.error("판매자 정보 조회 실패: memberId={}", memberId, error));
    }

    /**
     * 판매자 정보 수정
     * PUT /api/v1/sellers/me
     */
    @PutMapping("/me")
    public Mono<ApiResponse<SellerResponse>> updateSeller(
            @RequestAttribute("memberId") Long memberId,
            @Valid @RequestBody SellerUpdateRequest request
    ) {
        log.info("판매자 정보 수정 요청: memberId={}", memberId);

        return sellerService.updateSeller(memberId, request)
                .map(response -> ApiResponse.success(response, "판매자 정보가 수정되었습니다."))
                .doOnSuccess(response -> log.info("판매자 정보 수정 성공: memberId={}", memberId))
                .doOnError(error -> log.error("판매자 정보 수정 실패: memberId={}", memberId, error));
    }

    /**
     * 판매자 탈퇴
     * DELETE /api/v1/sellers/me
     */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> withdrawSeller(
            @RequestAttribute("memberId") Long memberId
    ) {
        log.info("판매자 탈퇴 요청: memberId={}", memberId);

        return sellerService.withdrawSeller(memberId)
                .doOnSuccess(v -> log.info("판매자 탈퇴 성공: memberId={}", memberId))
                .doOnError(error -> log.error("판매자 탈퇴 실패: memberId={}", memberId, error));
    }

    /**
     * 판매자 여부 확인
     * GET /api/v1/sellers/check
     */
    @GetMapping("/check")
    public Mono<ApiResponse<Boolean>> checkSeller(
            @RequestAttribute("memberId") Long memberId
    ) {
        log.info("판매자 여부 확인: memberId={}", memberId);

        return sellerService.isSeller(memberId)
                .map(isSeller -> ApiResponse.success(isSeller,
                        isSeller ? "판매자입니다." : "판매자가 아닙니다."));
    }
}
