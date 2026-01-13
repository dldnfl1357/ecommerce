package com.example.ecommerce.domain.seller.service;

import com.example.ecommerce.domain.seller.dto.request.SellerRegisterRequest;
import com.example.ecommerce.domain.seller.dto.request.SellerUpdateRequest;
import com.example.ecommerce.domain.seller.dto.response.SellerResponse;
import com.example.ecommerce.domain.seller.entity.Seller;
import com.example.ecommerce.domain.seller.entity.SellerStatus;
import com.example.ecommerce.domain.seller.repository.SellerRepository;
import com.example.ecommerce.global.exception.BusinessException;
import com.example.ecommerce.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SellerService 테스트")
class SellerServiceTest {

    @InjectMocks
    private SellerService sellerService;

    @Mock
    private SellerRepository sellerRepository;

    private Seller testSeller;
    private SellerRegisterRequest registerRequest;
    private SellerUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        testSeller = Seller.builder()
                .id(1L)
                .memberId(100L)
                .businessName("테스트 판매자")
                .businessNumber("1234567890")
                .representativeName("홍길동")
                .businessAddress("서울시 강남구")
                .contactNumber("0212345678")
                .status(SellerStatus.ACTIVE)
                .build();

        registerRequest = SellerRegisterRequest.builder()
                .businessName("테스트 판매자")
                .businessNumber("123-45-67890")
                .representativeName("홍길동")
                .businessAddress("서울시 강남구")
                .contactNumber("02-1234-5678")
                .build();

        updateRequest = SellerUpdateRequest.builder()
                .businessName("수정된 판매자")
                .businessAddress("서울시 서초구")
                .contactNumber("02-9999-8888")
                .build();
    }

    // ========== 판매자 등록 테스트 ==========

    @Nested
    @DisplayName("판매자 등록")
    class RegisterSeller {

        @Test
        @DisplayName("[성공] 판매자 등록이 완료된다")
        void registerSeller_success() {
            // given
            given(sellerRepository.existsByMemberId(100L))
                    .willReturn(Mono.just(false));
            given(sellerRepository.existsByBusinessNumber(anyString()))
                    .willReturn(Mono.just(false));
            given(sellerRepository.save(any(Seller.class)))
                    .willReturn(Mono.just(testSeller));

            // when
            Mono<SellerResponse> result = sellerService.registerSeller(100L, registerRequest);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.getBusinessName()).isEqualTo("테스트 판매자");
                        assertThat(response.getStatus()).isEqualTo(SellerStatus.ACTIVE);
                    })
                    .verifyComplete();

            verify(sellerRepository, times(1)).existsByMemberId(100L);
            verify(sellerRepository, times(1)).existsByBusinessNumber(anyString());
            verify(sellerRepository, times(1)).save(any(Seller.class));
        }

        @Test
        @DisplayName("[실패] 이미 판매자인 경우 등록 실패")
        void registerSeller_fail_alreadySeller() {
            // given
            given(sellerRepository.existsByMemberId(100L))
                    .willReturn(Mono.just(true));

            // when
            Mono<SellerResponse> result = sellerService.registerSeller(100L, registerRequest);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.ALREADY_SELLER
                    )
                    .verify();

            verify(sellerRepository, never()).save(any(Seller.class));
        }

        @Test
        @DisplayName("[실패] 사업자 등록번호 중복")
        void registerSeller_fail_duplicateBusinessNumber() {
            // given
            given(sellerRepository.existsByMemberId(100L))
                    .willReturn(Mono.just(false));
            given(sellerRepository.existsByBusinessNumber(anyString()))
                    .willReturn(Mono.just(true));

            // when
            Mono<SellerResponse> result = sellerService.registerSeller(100L, registerRequest);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.DUPLICATE_BUSINESS_NUMBER
                    )
                    .verify();

            verify(sellerRepository, never()).save(any(Seller.class));
        }

        @Test
        @DisplayName("[실패] DB 저장 오류")
        void registerSeller_fail_saveError() {
            // given
            given(sellerRepository.existsByMemberId(100L))
                    .willReturn(Mono.just(false));
            given(sellerRepository.existsByBusinessNumber(anyString()))
                    .willReturn(Mono.just(false));
            given(sellerRepository.save(any(Seller.class)))
                    .willReturn(Mono.error(new RuntimeException("Database error")));

            // when
            Mono<SellerResponse> result = sellerService.registerSeller(100L, registerRequest);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.INTERNAL_SERVER_ERROR
                    )
                    .verify();
        }

        @Test
        @DisplayName("[실패] 회원 존재 확인 중 오류")
        void registerSeller_fail_memberCheckError() {
            // given
            given(sellerRepository.existsByMemberId(100L))
                    .willReturn(Mono.error(new RuntimeException("Database error")));

            // when
            Mono<SellerResponse> result = sellerService.registerSeller(100L, registerRequest);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.INTERNAL_SERVER_ERROR
                    )
                    .verify();

            verify(sellerRepository, never()).save(any(Seller.class));
        }

        @Test
        @DisplayName("[실패] 사업자번호 중복 확인 중 오류")
        void registerSeller_fail_businessNumberCheckError() {
            // given
            given(sellerRepository.existsByMemberId(100L))
                    .willReturn(Mono.just(false));
            given(sellerRepository.existsByBusinessNumber(anyString()))
                    .willReturn(Mono.error(new RuntimeException("Database error")));

            // when
            Mono<SellerResponse> result = sellerService.registerSeller(100L, registerRequest);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.INTERNAL_SERVER_ERROR
                    )
                    .verify();
        }
    }

    // ========== 판매자 조회 테스트 ==========

    @Nested
    @DisplayName("판매자 정보 조회")
    class GetMySeller {

        @Test
        @DisplayName("[성공] 내 판매자 정보를 조회한다")
        void getMySeller_success() {
            // given
            given(sellerRepository.findByMemberId(100L))
                    .willReturn(Mono.just(testSeller));

            // when
            Mono<SellerResponse> result = sellerService.getMySeller(100L);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.getBusinessName()).isEqualTo("테스트 판매자");
                        assertThat(response.getMemberId()).isEqualTo(100L);
                    })
                    .verifyComplete();

            verify(sellerRepository, times(1)).findByMemberId(100L);
        }

        @Test
        @DisplayName("[실패] 판매자가 아닌 경우")
        void getMySeller_fail_notSeller() {
            // given
            given(sellerRepository.findByMemberId(100L))
                    .willReturn(Mono.empty());

            // when
            Mono<SellerResponse> result = sellerService.getMySeller(100L);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.SELLER_NOT_FOUND
                    )
                    .verify();
        }

        @Test
        @DisplayName("[실패] DB 조회 오류")
        void getMySeller_fail_databaseError() {
            // given
            given(sellerRepository.findByMemberId(100L))
                    .willReturn(Mono.error(new RuntimeException("Database error")));

            // when
            Mono<SellerResponse> result = sellerService.getMySeller(100L);

            // then
            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }

    // ========== 판매자 ID로 조회 테스트 ==========

    @Nested
    @DisplayName("판매자 ID로 조회")
    class GetSeller {

        @Test
        @DisplayName("[성공] 판매자 ID로 정보를 조회한다")
        void getSeller_success() {
            // given
            given(sellerRepository.findById(1L))
                    .willReturn(Mono.just(testSeller));

            // when
            Mono<SellerResponse> result = sellerService.getSeller(1L);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.getId()).isEqualTo(1L);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 판매자 ID")
        void getSeller_fail_notFound() {
            // given
            given(sellerRepository.findById(999L))
                    .willReturn(Mono.empty());

            // when
            Mono<SellerResponse> result = sellerService.getSeller(999L);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.SELLER_NOT_FOUND
                    )
                    .verify();
        }
    }

    // ========== 판매자 정보 수정 테스트 ==========

    @Nested
    @DisplayName("판매자 정보 수정")
    class UpdateSeller {

        @Test
        @DisplayName("[성공] 판매자 정보를 수정한다")
        void updateSeller_success() {
            // given
            given(sellerRepository.findByMemberId(100L))
                    .willReturn(Mono.just(testSeller));
            given(sellerRepository.save(any(Seller.class)))
                    .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            // when
            Mono<SellerResponse> result = sellerService.updateSeller(100L, updateRequest);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                    })
                    .verifyComplete();

            verify(sellerRepository, times(1)).findByMemberId(100L);
            verify(sellerRepository, times(1)).save(any(Seller.class));
        }

        @Test
        @DisplayName("[실패] 판매자가 아닌 경우 수정 실패")
        void updateSeller_fail_notSeller() {
            // given
            given(sellerRepository.findByMemberId(100L))
                    .willReturn(Mono.empty());

            // when
            Mono<SellerResponse> result = sellerService.updateSeller(100L, updateRequest);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.SELLER_NOT_FOUND
                    )
                    .verify();

            verify(sellerRepository, never()).save(any(Seller.class));
        }

        @Test
        @DisplayName("[실패] DB 저장 오류")
        void updateSeller_fail_saveError() {
            // given
            given(sellerRepository.findByMemberId(100L))
                    .willReturn(Mono.just(testSeller));
            given(sellerRepository.save(any(Seller.class)))
                    .willReturn(Mono.error(new RuntimeException("Save error")));

            // when
            Mono<SellerResponse> result = sellerService.updateSeller(100L, updateRequest);

            // then
            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }

    // ========== 판매자 탈퇴 테스트 ==========

    @Nested
    @DisplayName("판매자 탈퇴")
    class WithdrawSeller {

        @Test
        @DisplayName("[성공] 판매자 탈퇴가 완료된다")
        void withdrawSeller_success() {
            // given
            given(sellerRepository.findByMemberId(100L))
                    .willReturn(Mono.just(testSeller));
            given(sellerRepository.save(any(Seller.class)))
                    .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            // when
            Mono<Void> result = sellerService.withdrawSeller(100L);

            // then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(sellerRepository, times(1)).findByMemberId(100L);
            verify(sellerRepository, times(1)).save(any(Seller.class));
        }

        @Test
        @DisplayName("[실패] 판매자가 아닌 경우 탈퇴 실패")
        void withdrawSeller_fail_notSeller() {
            // given
            given(sellerRepository.findByMemberId(100L))
                    .willReturn(Mono.empty());

            // when
            Mono<Void> result = sellerService.withdrawSeller(100L);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.SELLER_NOT_FOUND
                    )
                    .verify();

            verify(sellerRepository, never()).save(any(Seller.class));
        }

        @Test
        @DisplayName("[실패] DB 저장 오류")
        void withdrawSeller_fail_saveError() {
            // given
            given(sellerRepository.findByMemberId(100L))
                    .willReturn(Mono.just(testSeller));
            given(sellerRepository.save(any(Seller.class)))
                    .willReturn(Mono.error(new RuntimeException("Save error")));

            // when
            Mono<Void> result = sellerService.withdrawSeller(100L);

            // then
            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }

    // ========== 판매자 여부 확인 테스트 ==========

    @Nested
    @DisplayName("판매자 여부 확인")
    class IsSeller {

        @Test
        @DisplayName("[성공] 판매자인 경우 true를 반환한다")
        void isSeller_success_true() {
            // given
            given(sellerRepository.existsByMemberId(100L))
                    .willReturn(Mono.just(true));

            // when
            Mono<Boolean> result = sellerService.isSeller(100L);

            // then
            StepVerifier.create(result)
                    .assertNext(isSeller -> {
                        assertThat(isSeller).isTrue();
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("[성공] 판매자가 아닌 경우 false를 반환한다")
        void isSeller_success_false() {
            // given
            given(sellerRepository.existsByMemberId(100L))
                    .willReturn(Mono.just(false));

            // when
            Mono<Boolean> result = sellerService.isSeller(100L);

            // then
            StepVerifier.create(result)
                    .assertNext(isSeller -> {
                        assertThat(isSeller).isFalse();
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("[실패] DB 조회 오류")
        void isSeller_fail_databaseError() {
            // given
            given(sellerRepository.existsByMemberId(100L))
                    .willReturn(Mono.error(new RuntimeException("Database error")));

            // when
            Mono<Boolean> result = sellerService.isSeller(100L);

            // then
            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }

    // ========== 활성 판매자 조회 테스트 ==========

    @Nested
    @DisplayName("활성 판매자 조회")
    class GetActiveSeller {

        @Test
        @DisplayName("[성공] 활성 판매자를 조회한다")
        void getActiveSeller_success() {
            // given
            given(sellerRepository.findActiveByMemberId(100L))
                    .willReturn(Mono.just(testSeller));

            // when
            Mono<Seller> result = sellerService.getActiveSeller(100L);

            // then
            StepVerifier.create(result)
                    .assertNext(seller -> {
                        assertThat(seller).isNotNull();
                        assertThat(seller.getId()).isEqualTo(1L);
                        assertThat(seller.getStatus()).isEqualTo(SellerStatus.ACTIVE);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("[실패] 판매자가 아닌 경우")
        void getActiveSeller_fail_notSeller() {
            // given
            given(sellerRepository.findActiveByMemberId(100L))
                    .willReturn(Mono.empty());

            // when
            Mono<Seller> result = sellerService.getActiveSeller(100L);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.SELLER_NOT_FOUND
                    )
                    .verify();
        }

        @Test
        @DisplayName("[실패] 비활성 판매자인 경우")
        void getActiveSeller_fail_inactiveSeller() {
            // given - 비활성 판매자는 findActiveByMemberId에서 조회되지 않음
            given(sellerRepository.findActiveByMemberId(100L))
                    .willReturn(Mono.empty());

            // when
            Mono<Seller> result = sellerService.getActiveSeller(100L);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.SELLER_NOT_FOUND
                    )
                    .verify();
        }

        @Test
        @DisplayName("[실패] DB 조회 오류")
        void getActiveSeller_fail_databaseError() {
            // given
            given(sellerRepository.findActiveByMemberId(100L))
                    .willReturn(Mono.error(new RuntimeException("Database error")));

            // when
            Mono<Seller> result = sellerService.getActiveSeller(100L);

            // then
            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }
}
