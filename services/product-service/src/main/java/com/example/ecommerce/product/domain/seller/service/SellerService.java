package com.example.ecommerce.product.domain.seller.service;

import com.example.ecommerce.common.exception.BusinessException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.product.domain.seller.dto.request.SellerRegisterRequest;
import com.example.ecommerce.product.domain.seller.dto.response.SellerResponse;
import com.example.ecommerce.product.domain.seller.entity.Seller;
import com.example.ecommerce.product.domain.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerService {

    private final SellerRepository sellerRepository;

    @Transactional
    public Mono<SellerResponse> registerSeller(Long memberId, SellerRegisterRequest request) {
        return validateNotAlreadySeller(memberId)
                .then(validateBusinessNumber(request.getNormalizedBusinessNumber()))
                .then(Mono.defer(() -> {
                    Seller seller = Seller.builder()
                            .memberId(memberId)
                            .businessName(request.getBusinessName())
                            .businessNumber(request.getNormalizedBusinessNumber())
                            .representativeName(request.getRepresentativeName())
                            .contactPhone(request.getContactPhone())
                            .contactEmail(request.getContactEmail())
                            .build();
                    return sellerRepository.save(seller);
                }))
                .map(SellerResponse::from)
                .doOnSuccess(response -> log.info("판매자 등록 완료: memberId={}, sellerId={}", memberId, response.getId()));
    }

    public Mono<SellerResponse> getSeller(Long sellerId) {
        return sellerRepository.findById(sellerId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.SELLER_NOT_FOUND)))
                .map(SellerResponse::from);
    }

    public Mono<SellerResponse> getSellerByMemberId(Long memberId) {
        return sellerRepository.findByMemberId(memberId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.SELLER_NOT_FOUND)))
                .map(SellerResponse::from);
    }

    @Transactional
    public Mono<SellerResponse> approveSeller(Long sellerId) {
        return sellerRepository.findById(sellerId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.SELLER_NOT_FOUND)))
                .map(Seller::approve)
                .flatMap(sellerRepository::save)
                .map(SellerResponse::from)
                .doOnSuccess(response -> log.info("판매자 승인 완료: sellerId={}", sellerId));
    }

    @Transactional
    public Mono<SellerResponse> suspendSeller(Long sellerId) {
        return sellerRepository.findById(sellerId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.SELLER_NOT_FOUND)))
                .map(Seller::suspend)
                .flatMap(sellerRepository::save)
                .map(SellerResponse::from)
                .doOnSuccess(response -> log.info("판매자 정지: sellerId={}", sellerId));
    }

    private Mono<Void> validateNotAlreadySeller(Long memberId) {
        return sellerRepository.existsByMemberId(memberId)
                .flatMap(exists -> exists
                        ? Mono.error(new BusinessException(ErrorCode.ALREADY_SELLER))
                        : Mono.empty());
    }

    private Mono<Void> validateBusinessNumber(String businessNumber) {
        return sellerRepository.existsByBusinessNumber(businessNumber)
                .flatMap(exists -> exists
                        ? Mono.error(new BusinessException(ErrorCode.DUPLICATE_BUSINESS_NUMBER))
                        : Mono.empty());
    }
}
