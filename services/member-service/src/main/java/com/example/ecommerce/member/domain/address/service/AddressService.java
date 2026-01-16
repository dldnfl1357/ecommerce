package com.example.ecommerce.member.domain.address.service;

import com.example.ecommerce.common.exception.BusinessException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.member.domain.address.dto.request.AddressCreateRequest;
import com.example.ecommerce.member.domain.address.dto.response.AddressResponse;
import com.example.ecommerce.member.domain.address.entity.Address;
import com.example.ecommerce.member.domain.address.repository.AddressRepository;
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

    @Transactional
    public Mono<AddressResponse> createAddress(Long memberId, AddressCreateRequest request) {
        return validateAddressCount(memberId)
                .then(determineDefault(memberId, request.getIsDefault()))
                .flatMap(isDefault -> {
                    Address address = Address.builder()
                            .memberId(memberId)
                            .name(request.getName())
                            .recipient(request.getRecipient())
                            .phone(request.getNormalizedPhone())
                            .zipCode(request.getZipCode())
                            .address(request.getAddress())
                            .addressDetail(request.getAddressDetail())
                            .isDefault(isDefault)
                            .deliveryRequest(request.getDeliveryRequest())
                            .build();

                    if (isDefault) {
                        return clearDefaultAddress(memberId)
                                .then(addressRepository.save(address));
                    }
                    return addressRepository.save(address);
                })
                .map(AddressResponse::from)
                .doOnSuccess(response -> log.info("배송지 생성 완료: memberId={}, addressId={}", memberId, response.getId()));
    }

    public Flux<AddressResponse> getAddresses(Long memberId) {
        return addressRepository.findByMemberIdOrderByIsDefaultDescCreatedAtDesc(memberId)
                .map(AddressResponse::from);
    }

    public Mono<AddressResponse> getAddress(Long memberId, Long addressId) {
        return addressRepository.findById(addressId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.ADDRESS_NOT_FOUND)))
                .flatMap(address -> validateOwnership(address, memberId))
                .map(AddressResponse::from);
    }

    public Mono<AddressResponse> getDefaultAddress(Long memberId) {
        return addressRepository.findByMemberIdAndIsDefaultTrue(memberId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.DEFAULT_ADDRESS_NOT_FOUND)))
                .map(AddressResponse::from);
    }

    @Transactional
    public Mono<AddressResponse> setDefaultAddress(Long memberId, Long addressId) {
        return addressRepository.findById(addressId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.ADDRESS_NOT_FOUND)))
                .flatMap(address -> validateOwnership(address, memberId))
                .flatMap(address -> clearDefaultAddress(memberId)
                        .then(addressRepository.save(address.setAsDefault())))
                .map(AddressResponse::from)
                .doOnSuccess(response -> log.info("기본 배송지 설정: memberId={}, addressId={}", memberId, addressId));
    }

    @Transactional
    public Mono<Void> deleteAddress(Long memberId, Long addressId) {
        return addressRepository.findById(addressId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.ADDRESS_NOT_FOUND)))
                .flatMap(address -> validateOwnership(address, memberId))
                .flatMap(address -> {
                    if (address.getIsDefault()) {
                        return addressRepository.countByMemberId(memberId)
                                .flatMap(count -> {
                                    if (count > 1) {
                                        return Mono.error(new BusinessException(ErrorCode.CANNOT_DELETE_DEFAULT_ADDRESS));
                                    }
                                    return addressRepository.delete(address);
                                });
                    }
                    return addressRepository.delete(address);
                })
                .doOnSuccess(v -> log.info("배송지 삭제 완료: memberId={}, addressId={}", memberId, addressId));
    }

    private Mono<Void> validateAddressCount(Long memberId) {
        return addressRepository.countByMemberId(memberId)
                .flatMap(count -> {
                    if (count >= MAX_ADDRESS_COUNT) {
                        return Mono.error(new BusinessException(ErrorCode.MAX_ADDRESS_COUNT_EXCEEDED));
                    }
                    return Mono.empty();
                });
    }

    private Mono<Boolean> determineDefault(Long memberId, Boolean requestedDefault) {
        return addressRepository.countByMemberId(memberId)
                .map(count -> count == 0 || Boolean.TRUE.equals(requestedDefault));
    }

    private Mono<Void> clearDefaultAddress(Long memberId) {
        return addressRepository.findByMemberIdAndIsDefaultTrue(memberId)
                .flatMap(address -> addressRepository.save(address.unsetDefault()))
                .then();
    }

    private Mono<Address> validateOwnership(Address address, Long memberId) {
        if (!address.getMemberId().equals(memberId)) {
            return Mono.error(new BusinessException(ErrorCode.UNAUTHORIZED_ADDRESS_ACCESS));
        }
        return Mono.just(address);
    }
}
