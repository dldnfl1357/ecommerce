package com.example.ecommerce.domain.member.service;

import com.example.ecommerce.domain.member.dto.request.AddressCreateRequest;
import com.example.ecommerce.domain.member.dto.response.AddressResponse;
import com.example.ecommerce.domain.member.entity.Address;
import com.example.ecommerce.domain.member.repository.AddressRepository;
import com.example.ecommerce.global.exception.BusinessException;
import com.example.ecommerce.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private static final int MAX_ADDRESS_COUNT = 10;

    /**
     * 배송지 생성
     */
    @Transactional
    public Mono<AddressResponse> createAddress(Long memberId, AddressCreateRequest request) {
        return validateAddressCount(memberId)
            .then(createAddressEntity(memberId, request))
            .flatMap(address -> handleDefaultAddress(address, memberId))
            .flatMap(addressRepository::save)
            .doOnSuccess(address -> log.info("배송지 생성 완료: memberId={}, addressId={}",
                memberId, address.getId()))
            .map(AddressResponse::from);
    }

    /**
     * 배송지 목록 조회
     */
    public Flux<AddressResponse> getAddresses(Long memberId) {
        return addressRepository.findByMemberId(memberId)
            .sort((a1, a2) -> {
                // 기본 배송지를 먼저 정렬
                if (a1.getIsDefault() && !a2.getIsDefault()) return -1;
                if (!a1.getIsDefault() && a2.getIsDefault()) return 1;
                return 0;
            })
            .map(AddressResponse::from);
    }

    /**
     * 기본 배송지 조회
     */
    public Mono<AddressResponse> getDefaultAddress(Long memberId) {
        return addressRepository.findByMemberIdAndIsDefaultTrue(memberId)
            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.DEFAULT_ADDRESS_NOT_FOUND)))
            .map(AddressResponse::from);
    }

    /**
     * 배송지 조회 (단건)
     */
    public Mono<AddressResponse> getAddress(Long addressId, Long memberId) {
        return addressRepository.findById(addressId)
            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.ADDRESS_NOT_FOUND)))
            .flatMap(address -> validateOwner(address, memberId))
            .map(AddressResponse::from);
    }

    /**
     * 기본 배송지 설정
     */
    @Transactional
    public Mono<AddressResponse> setDefaultAddress(Long addressId, Long memberId) {
        return addressRepository.findById(addressId)
            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.ADDRESS_NOT_FOUND)))
            .flatMap(address -> validateOwner(address, memberId))
            .flatMap(address ->
                // 기존 기본 배송지 해제
                addressRepository.unsetDefaultByMemberId(memberId)
                    .then(Mono.just(address.setAsDefault()))
            )
            .flatMap(addressRepository::save)
            .doOnSuccess(address -> log.info("기본 배송지 설정: memberId={}, addressId={}",
                memberId, addressId))
            .map(AddressResponse::from);
    }

    /**
     * 배송지 삭제
     */
    @Transactional
    public Mono<Void> deleteAddress(Long addressId, Long memberId) {
        return addressRepository.findById(addressId)
            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.ADDRESS_NOT_FOUND)))
            .flatMap(address -> validateOwner(address, memberId))
            .flatMap(address -> {
                // 기본 배송지 삭제 방지 검증
                if (address.getIsDefault()) {
                    return addressRepository.countByMemberId(memberId)
                        .flatMap(count -> count > 1
                            ? Mono.error(new BusinessException(ErrorCode.CANNOT_DELETE_DEFAULT_ADDRESS))
                            : Mono.just(address));
                }
                return Mono.just(address);
            })
            .flatMap(address -> addressRepository.deleteById(address.getId()))
            .doOnSuccess(v -> log.info("배송지 삭제 완료: addressId={}", addressId));
    }

    // ========== Private Methods ==========

    private Mono<Void> validateAddressCount(Long memberId) {
        return addressRepository.countByMemberId(memberId)
            .flatMap(count -> count >= MAX_ADDRESS_COUNT
                ? Mono.error(new BusinessException(ErrorCode.MAX_ADDRESS_COUNT_EXCEEDED))
                : Mono.empty());
    }

    private Mono<Address> createAddressEntity(Long memberId, AddressCreateRequest request) {
        Address address = Address.builder()
            .memberId(memberId)
            .name(request.getName())
            .recipient(request.getRecipient())
            .phone(request.getNormalizedPhone())
            .zipCode(request.getZipCode())
            .address(request.getAddress())
            .addressDetail(request.getAddressDetail())
            .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
            .deliveryRequest(request.getDeliveryRequest())
            .build();

        return Mono.just(address);
    }

    private Mono<Address> handleDefaultAddress(Address address, Long memberId) {
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            // 기존 기본 배송지 해제
            return addressRepository.unsetDefaultByMemberId(memberId)
                .thenReturn(address);
        }

        // 첫 번째 배송지면 자동으로 기본 설정
        return addressRepository.countByMemberId(memberId)
            .map(count -> count == 0 ? address.setAsDefault() : address);
    }

    private Mono<Address> validateOwner(Address address, Long memberId) {
        if (!address.getMemberId().equals(memberId)) {
            return Mono.error(new BusinessException(ErrorCode.UNAUTHORIZED_ADDRESS_ACCESS));
        }
        return Mono.just(address);
    }
}
