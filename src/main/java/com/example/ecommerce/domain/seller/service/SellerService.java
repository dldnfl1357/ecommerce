package com.example.ecommerce.domain.seller.service;

import com.example.ecommerce.domain.seller.dto.request.SellerRegisterRequest;
import com.example.ecommerce.domain.seller.dto.request.SellerUpdateRequest;
import com.example.ecommerce.domain.seller.dto.response.SellerResponse;
import com.example.ecommerce.domain.seller.entity.Seller;
import com.example.ecommerce.domain.seller.repository.SellerRepository;
import com.example.ecommerce.global.exception.BusinessException;
import com.example.ecommerce.global.exception.ErrorCode;
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

    /**
     * 판매자 등록
     */
    @Transactional
    public Mono<SellerResponse> registerSeller(Long memberId, SellerRegisterRequest request) {
        return validateNotAlreadySeller(memberId)
                .then(validateBusinessNumberNotDuplicate(request.getNormalizedBusinessNumber()))
                .then(Mono.just(request.toEntity(memberId)))
                .flatMap(sellerRepository::save)
                .doOnSuccess(seller -> log.info("판매자 등록 완료: sellerId={}, memberId={}",
                        seller.getId(), memberId))
                .map(SellerResponse::from)
                .onErrorMap(this::mapToBusinessException);
    }

    /**
     * 내 판매자 정보 조회
     */
    public Mono<SellerResponse> getMySeller(Long memberId) {
        return sellerRepository.findByMemberId(memberId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.SELLER_NOT_FOUND)))
                .doOnSuccess(seller -> log.info("판매자 정보 조회: sellerId={}, memberId={}",
                        seller.getId(), memberId))
                .map(SellerResponse::from);
    }

    /**
     * 판매자 ID로 조회
     */
    public Mono<SellerResponse> getSeller(Long sellerId) {
        return sellerRepository.findById(sellerId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.SELLER_NOT_FOUND)))
                .map(SellerResponse::from);
    }

    /**
     * 판매자 정보 수정
     */
    @Transactional
    public Mono<SellerResponse> updateSeller(Long memberId, SellerUpdateRequest request) {
        return sellerRepository.findByMemberId(memberId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.SELLER_NOT_FOUND)))
                .map(seller -> seller.updateInfo(
                        request.getBusinessName(),
                        request.getBusinessAddress(),
                        request.getNormalizedContactNumber()
                ))
                .flatMap(sellerRepository::save)
                .doOnSuccess(seller -> log.info("판매자 정보 수정: sellerId={}", seller.getId()))
                .map(SellerResponse::from);
    }

    /**
     * 판매자 탈퇴
     */
    @Transactional
    public Mono<Void> withdrawSeller(Long memberId) {
        return sellerRepository.findByMemberId(memberId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.SELLER_NOT_FOUND)))
                .map(Seller::withdraw)
                .flatMap(sellerRepository::save)
                .doOnSuccess(seller -> log.info("판매자 탈퇴: sellerId={}", seller.getId()))
                .then();
    }

    /**
     * 판매자 여부 확인
     */
    public Mono<Boolean> isSeller(Long memberId) {
        return sellerRepository.existsByMemberId(memberId);
    }

    /**
     * 활성 판매자 조회 (상품 등록 시 검증용)
     */
    public Mono<Seller> getActiveSeller(Long memberId) {
        return sellerRepository.findActiveByMemberId(memberId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.SELLER_NOT_FOUND)));
    }

    // ========== Private Helper Methods ==========

    private Mono<Void> validateNotAlreadySeller(Long memberId) {
        return sellerRepository.existsByMemberId(memberId)
                .flatMap(exists -> exists
                        ? Mono.error(new BusinessException(ErrorCode.ALREADY_SELLER))
                        : Mono.empty());
    }

    private Mono<Void> validateBusinessNumberNotDuplicate(String businessNumber) {
        return sellerRepository.existsByBusinessNumber(businessNumber)
                .flatMap(exists -> exists
                        ? Mono.error(new BusinessException(ErrorCode.DUPLICATE_BUSINESS_NUMBER))
                        : Mono.empty());
    }

    private Throwable mapToBusinessException(Throwable error) {
        if (error instanceof BusinessException) {
            return error;
        }
        log.error("예상치 못한 오류 발생", error);
        return new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
