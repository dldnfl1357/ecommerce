package com.example.ecommerce.member.domain.address.controller;

import com.example.ecommerce.common.response.ApiResponse;
import com.example.ecommerce.member.domain.address.dto.request.AddressCreateRequest;
import com.example.ecommerce.member.domain.address.dto.response.AddressResponse;
import com.example.ecommerce.member.domain.address.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<AddressResponse>> createAddress(
            @RequestHeader("X-Member-Id") Long memberId,
            @Valid @RequestBody AddressCreateRequest request
    ) {
        log.info("배송지 생성 요청: memberId={}", memberId);
        return addressService.createAddress(memberId, request)
                .map(response -> ApiResponse.success(response, "배송지가 등록되었습니다."));
    }

    @GetMapping
    public Mono<ApiResponse<List<AddressResponse>>> getAddresses(
            @RequestHeader("X-Member-Id") Long memberId
    ) {
        return addressService.getAddresses(memberId)
                .collectList()
                .map(ApiResponse::success);
    }

    @GetMapping("/{addressId}")
    public Mono<ApiResponse<AddressResponse>> getAddress(
            @RequestHeader("X-Member-Id") Long memberId,
            @PathVariable Long addressId
    ) {
        return addressService.getAddress(memberId, addressId)
                .map(ApiResponse::success);
    }

    @GetMapping("/default")
    public Mono<ApiResponse<AddressResponse>> getDefaultAddress(
            @RequestHeader("X-Member-Id") Long memberId
    ) {
        return addressService.getDefaultAddress(memberId)
                .map(ApiResponse::success);
    }

    @PutMapping("/{addressId}/default")
    public Mono<ApiResponse<AddressResponse>> setDefaultAddress(
            @RequestHeader("X-Member-Id") Long memberId,
            @PathVariable Long addressId
    ) {
        log.info("기본 배송지 설정 요청: memberId={}, addressId={}", memberId, addressId);
        return addressService.setDefaultAddress(memberId, addressId)
                .map(response -> ApiResponse.success(response, "기본 배송지로 설정되었습니다."));
    }

    @DeleteMapping("/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteAddress(
            @RequestHeader("X-Member-Id") Long memberId,
            @PathVariable Long addressId
    ) {
        log.info("배송지 삭제 요청: memberId={}, addressId={}", memberId, addressId);
        return addressService.deleteAddress(memberId, addressId);
    }
}
