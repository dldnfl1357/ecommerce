package com.example.ecommerce.domain.member.controller;

import com.example.ecommerce.domain.member.dto.request.AddressCreateRequest;
import com.example.ecommerce.domain.member.dto.request.MemberUpdateRequest;
import com.example.ecommerce.domain.member.dto.response.AddressResponse;
import com.example.ecommerce.domain.member.dto.response.MemberResponse;
import com.example.ecommerce.domain.member.service.AddressService;
import com.example.ecommerce.domain.member.service.MemberService;
import com.example.ecommerce.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 회원 관리 API Controller (WebFlux)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final AddressService addressService;

    /**
     * 내 정보 조회
     * GET /api/v1/members/me
     */
    @GetMapping("/me")
    public Mono<ApiResponse<MemberResponse>> getMyInfo(
        @RequestAttribute("memberId") Long memberId
    ) {
        log.info("내 정보 조회 요청: memberId={}", memberId);

        return memberService.getMember(memberId)
            .map(ApiResponse::success)
            .doOnSuccess(response -> log.info("내 정보 조회 성공: memberId={}", memberId))
            .doOnError(error -> log.error("내 정보 조회 실패: memberId={}", memberId, error));
    }

    /**
     * 내 정보 수정
     * PUT /api/v1/members/me
     */
    @PutMapping("/me")
    public Mono<ApiResponse<MemberResponse>> updateMyInfo(
        @RequestAttribute("memberId") Long memberId,
        @Valid @RequestBody MemberUpdateRequest request
    ) {
        log.info("내 정보 수정 요청: memberId={}", memberId);

        return memberService.updateMember(memberId, request)
            .map(response -> ApiResponse.success(response, "회원 정보가 수정되었습니다."))
            .doOnSuccess(response -> log.info("내 정보 수정 성공: memberId={}", memberId))
            .doOnError(error -> log.error("내 정보 수정 실패: memberId={}", memberId, error));
    }

    /**
     * 회원 탈퇴
     * DELETE /api/v1/members/me
     */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<ApiResponse<Void>> withdraw(
        @RequestAttribute("memberId") Long memberId
    ) {
        log.info("회원 탈퇴 요청: memberId={}", memberId);

        return memberService.withdraw(memberId)
            .then(Mono.just(ApiResponse.success("회원 탈퇴가 완료되었습니다.")))
            .doOnSuccess(response -> log.info("회원 탈퇴 성공: memberId={}", memberId))
            .doOnError(error -> log.error("회원 탈퇴 실패: memberId={}", memberId, error));
    }

    /**
     * 로켓와우 구독
     * POST /api/v1/members/me/rocket-wow
     */
    @PostMapping("/me/rocket-wow")
    public Mono<ApiResponse<MemberResponse>> subscribeRocketWow(
        @RequestAttribute("memberId") Long memberId
    ) {
        log.info("로켓와우 구독 요청: memberId={}", memberId);

        return memberService.subscribeRocketWow(memberId)
            .map(response -> ApiResponse.success(response, "로켓와우 구독이 완료되었습니다."))
            .doOnSuccess(response -> log.info("로켓와우 구독 성공: memberId={}", memberId))
            .doOnError(error -> log.error("로켓와우 구독 실패: memberId={}", memberId, error));
    }

    /**
     * 로켓와우 취소
     * DELETE /api/v1/members/me/rocket-wow
     */
    @DeleteMapping("/me/rocket-wow")
    public Mono<ApiResponse<MemberResponse>> cancelRocketWow(
        @RequestAttribute("memberId") Long memberId
    ) {
        log.info("로켓와우 취소 요청: memberId={}", memberId);

        return memberService.cancelRocketWow(memberId)
            .map(response -> ApiResponse.success(response, "로켓와우 구독이 취소되었습니다."))
            .doOnSuccess(response -> log.info("로켓와우 취소 성공: memberId={}", memberId))
            .doOnError(error -> log.error("로켓와우 취소 실패: memberId={}", memberId, error));
    }

    // ========== Address API ==========

    /**
     * 배송지 목록 조회
     * GET /api/v1/members/me/addresses
     */
    @GetMapping("/me/addresses")
    public Mono<ApiResponse<Flux<AddressResponse>>> getAddresses(
        @RequestAttribute("memberId") Long memberId
    ) {
        log.info("배송지 목록 조회 요청: memberId={}", memberId);

        Flux<AddressResponse> addresses = addressService.getAddresses(memberId)
            .doOnComplete(() -> log.info("배송지 목록 조회 성공: memberId={}", memberId))
            .doOnError(error -> log.error("배송지 목록 조회 실패: memberId={}", memberId, error));

        return Mono.just(ApiResponse.success(addresses));
    }

    /**
     * 기본 배송지 조회
     * GET /api/v1/members/me/addresses/default
     */
    @GetMapping("/me/addresses/default")
    public Mono<ApiResponse<AddressResponse>> getDefaultAddress(
        @RequestAttribute("memberId") Long memberId
    ) {
        log.info("기본 배송지 조회 요청: memberId={}", memberId);

        return addressService.getDefaultAddress(memberId)
            .map(ApiResponse::success)
            .doOnSuccess(response -> log.info("기본 배송지 조회 성공: memberId={}", memberId))
            .doOnError(error -> log.error("기본 배송지 조회 실패: memberId={}", memberId, error));
    }

    /**
     * 배송지 추가
     * POST /api/v1/members/me/addresses
     */
    @PostMapping("/me/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<AddressResponse>> createAddress(
        @RequestAttribute("memberId") Long memberId,
        @Valid @RequestBody AddressCreateRequest request
    ) {
        log.info("배송지 추가 요청: memberId={}", memberId);

        return addressService.createAddress(memberId, request)
            .map(response -> ApiResponse.success(response, "배송지가 추가되었습니다."))
            .doOnSuccess(response -> log.info("배송지 추가 성공: memberId={}", memberId))
            .doOnError(error -> log.error("배송지 추가 실패: memberId={}", memberId, error));
    }

    /**
     * 배송지 조회 (단건)
     * GET /api/v1/members/me/addresses/{addressId}
     */
    @GetMapping("/me/addresses/{addressId}")
    public Mono<ApiResponse<AddressResponse>> getAddress(
        @RequestAttribute("memberId") Long memberId,
        @PathVariable Long addressId
    ) {
        log.info("배송지 조회 요청: memberId={}, addressId={}", memberId, addressId);

        return addressService.getAddress(addressId, memberId)
            .map(ApiResponse::success)
            .doOnSuccess(response -> log.info("배송지 조회 성공: addressId={}", addressId))
            .doOnError(error -> log.error("배송지 조회 실패: addressId={}", addressId, error));
    }

    /**
     * 기본 배송지 설정
     * PUT /api/v1/members/me/addresses/{addressId}/default
     */
    @PutMapping("/me/addresses/{addressId}/default")
    public Mono<ApiResponse<AddressResponse>> setDefaultAddress(
        @RequestAttribute("memberId") Long memberId,
        @PathVariable Long addressId
    ) {
        log.info("기본 배송지 설정 요청: memberId={}, addressId={}", memberId, addressId);

        return addressService.setDefaultAddress(addressId, memberId)
            .map(response -> ApiResponse.success(response, "기본 배송지로 설정되었습니다."))
            .doOnSuccess(response -> log.info("기본 배송지 설정 성공: addressId={}", addressId))
            .doOnError(error -> log.error("기본 배송지 설정 실패: addressId={}", addressId, error));
    }

    /**
     * 배송지 삭제
     * DELETE /api/v1/members/me/addresses/{addressId}
     */
    @DeleteMapping("/me/addresses/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<ApiResponse<Void>> deleteAddress(
        @RequestAttribute("memberId") Long memberId,
        @PathVariable Long addressId
    ) {
        log.info("배송지 삭제 요청: memberId={}, addressId={}", memberId, addressId);

        return addressService.deleteAddress(addressId, memberId)
            .then(Mono.just(ApiResponse.success("배송지가 삭제되었습니다.")))
            .doOnSuccess(response -> log.info("배송지 삭제 성공: addressId={}", addressId))
            .doOnError(error -> log.error("배송지 삭제 실패: addressId={}", addressId, error));
    }
}
