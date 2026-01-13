package com.example.ecommerce.domain.member.service;

import com.example.ecommerce.domain.member.dto.request.AddressCreateRequest;
import com.example.ecommerce.domain.member.dto.response.AddressResponse;
import com.example.ecommerce.domain.member.entity.Address;
import com.example.ecommerce.domain.member.repository.AddressRepository;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddressService 테스트")
class AddressServiceTest {

    @InjectMocks
    private AddressService addressService;

    @Mock
    private AddressRepository addressRepository;

    private Address testAddress;
    private AddressCreateRequest createRequest;
    private static final Long MEMBER_ID = 1L;
    private static final Long ADDRESS_ID = 1L;

    @BeforeEach
    void setUp() {
        testAddress = Address.builder()
            .id(ADDRESS_ID)
            .memberId(MEMBER_ID)
            .name("집")
            .recipient("홍길동")
            .phone("01012345678")
            .zipCode("12345")
            .address("서울시 강남구 테헤란로 123")
            .addressDetail("101동 1001호")
            .isDefault(true)
            .build();

        createRequest = AddressCreateRequest.builder()
            .name("집")
            .recipient("홍길동")
            .phone("010-1234-5678")
            .zipCode("12345")
            .address("서울시 강남구 테헤란로 123")
            .addressDetail("101동 1001호")
            .isDefault(false)
            .build();
    }

    // ========== 배송지 생성 테스트 ==========

    @Nested
    @DisplayName("배송지 생성")
    class CreateAddress {

        @Test
        @DisplayName("[성공] 배송지 생성 - 정상적으로 배송지가 생성된다")
        void createAddress_success() {
            // given
            given(addressRepository.countByMemberId(MEMBER_ID))
                .willReturn(Mono.just(0L));
            given(addressRepository.save(any(Address.class)))
                .willReturn(Mono.just(testAddress));

            // when
            Mono<AddressResponse> result = addressService.createAddress(MEMBER_ID, createRequest);

            // then
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getName()).isEqualTo("집");
                    assertThat(response.getRecipient()).isEqualTo("홍길동");
                    assertThat(response.getZipCode()).isEqualTo("12345");
                })
                .verifyComplete();

            verify(addressRepository, times(2)).countByMemberId(MEMBER_ID);
            verify(addressRepository, times(1)).save(any(Address.class));
        }

        @Test
        @DisplayName("[성공] 첫 번째 배송지는 자동으로 기본 배송지로 설정된다")
        void createAddress_firstAddressBecomesDefault() {
            // given
            given(addressRepository.countByMemberId(MEMBER_ID))
                .willReturn(Mono.just(0L));
            given(addressRepository.save(any(Address.class)))
                .willAnswer(invocation -> {
                    Address saved = invocation.getArgument(0);
                    return Mono.just(saved);
                });

            // when
            Mono<AddressResponse> result = addressService.createAddress(MEMBER_ID, createRequest);

            // then
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getIsDefault()).isTrue();
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("[실패] 배송지 생성 - 최대 개수(10개) 초과")
        void createAddress_fail_maxCountExceeded() {
            // given
            given(addressRepository.countByMemberId(MEMBER_ID))
                .willReturn(Mono.just(10L));

            // when
            Mono<AddressResponse> result = addressService.createAddress(MEMBER_ID, createRequest);

            // then
            StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                    throwable instanceof BusinessException &&
                    ((BusinessException) throwable).getErrorCode() == ErrorCode.MAX_ADDRESS_COUNT_EXCEEDED
                )
                .verify();

            verify(addressRepository, times(0)).save(any(Address.class));
        }

        @Test
        @DisplayName("[실패] 배송지 생성 - DB 저장 실패")
        void createAddress_fail_databaseError() {
            // given
            given(addressRepository.countByMemberId(MEMBER_ID))
                .willReturn(Mono.just(0L));
            given(addressRepository.save(any(Address.class)))
                .willReturn(Mono.error(new RuntimeException("Database error")));

            // when
            Mono<AddressResponse> result = addressService.createAddress(MEMBER_ID, createRequest);

            // then
            StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
        }

        @Test
        @DisplayName("[성공] 기본 배송지로 지정하면 기존 기본 배송지가 해제된다")
        void createAddress_unsetPreviousDefault() {
            // given
            AddressCreateRequest requestWithDefault = AddressCreateRequest.builder()
                .name("회사")
                .recipient("홍길동")
                .phone("010-1234-5678")
                .zipCode("54321")
                .address("서울시 서초구 서초대로 456")
                .addressDetail("A동 502호")
                .isDefault(true)
                .build();

            given(addressRepository.countByMemberId(MEMBER_ID))
                .willReturn(Mono.just(1L));
            given(addressRepository.unsetDefaultByMemberId(MEMBER_ID))
                .willReturn(Mono.just(1));
            given(addressRepository.save(any(Address.class)))
                .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            // when
            Mono<AddressResponse> result = addressService.createAddress(MEMBER_ID, requestWithDefault);

            // then
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getIsDefault()).isTrue();
                })
                .verifyComplete();

            verify(addressRepository, times(1)).unsetDefaultByMemberId(MEMBER_ID);
        }

        @Test
        @DisplayName("[실패] 배송지 생성 - 개수 조회 중 오류")
        void createAddress_fail_countError() {
            // given
            given(addressRepository.countByMemberId(MEMBER_ID))
                .willReturn(Mono.error(new RuntimeException("Count error")));

            // when
            Mono<AddressResponse> result = addressService.createAddress(MEMBER_ID, createRequest);

            // then
            StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

            verify(addressRepository, times(0)).save(any(Address.class));
        }
    }

    // ========== 배송지 목록 조회 테스트 ==========

    @Nested
    @DisplayName("배송지 목록 조회")
    class GetAddresses {

        @Test
        @DisplayName("[성공] 배송지 목록 조회 - 기본 배송지가 먼저 정렬된다")
        void getAddresses_success() {
            // given
            Address defaultAddress = Address.builder()
                .memberId(MEMBER_ID)
                .name("집")
                .recipient("홍길동")
                .phone("01012345678")
                .zipCode("12345")
                .address("서울시 강남구")
                .isDefault(true)
                .build();

            Address otherAddress = Address.builder()
                .memberId(MEMBER_ID)
                .name("회사")
                .recipient("홍길동")
                .phone("01012345678")
                .zipCode("54321")
                .address("서울시 서초구")
                .isDefault(false)
                .build();

            given(addressRepository.findByMemberId(MEMBER_ID))
                .willReturn(Flux.just(otherAddress, defaultAddress));

            // when
            Flux<AddressResponse> result = addressService.getAddresses(MEMBER_ID);

            // then
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getName()).isEqualTo("집");
                    assertThat(response.getIsDefault()).isTrue();
                })
                .assertNext(response -> {
                    assertThat(response.getName()).isEqualTo("회사");
                    assertThat(response.getIsDefault()).isFalse();
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("[성공] 배송지 목록 조회 - 배송지가 없으면 빈 목록 반환")
        void getAddresses_empty() {
            // given
            given(addressRepository.findByMemberId(MEMBER_ID))
                .willReturn(Flux.empty());

            // when
            Flux<AddressResponse> result = addressService.getAddresses(MEMBER_ID);

            // then
            StepVerifier.create(result)
                .verifyComplete();
        }
    }

    // ========== 기본 배송지 조회 테스트 ==========

    @Nested
    @DisplayName("기본 배송지 조회")
    class GetDefaultAddress {

        @Test
        @DisplayName("[성공] 기본 배송지 조회")
        void getDefaultAddress_success() {
            // given
            given(addressRepository.findByMemberIdAndIsDefaultTrue(MEMBER_ID))
                .willReturn(Mono.just(testAddress));

            // when
            Mono<AddressResponse> result = addressService.getDefaultAddress(MEMBER_ID);

            // then
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getIsDefault()).isTrue();
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("[실패] 기본 배송지 조회 - 기본 배송지 없음")
        void getDefaultAddress_fail_notFound() {
            // given
            given(addressRepository.findByMemberIdAndIsDefaultTrue(MEMBER_ID))
                .willReturn(Mono.empty());

            // when
            Mono<AddressResponse> result = addressService.getDefaultAddress(MEMBER_ID);

            // then
            StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                    throwable instanceof BusinessException &&
                    ((BusinessException) throwable).getErrorCode() == ErrorCode.DEFAULT_ADDRESS_NOT_FOUND
                )
                .verify();
        }
    }

    // ========== 배송지 단건 조회 테스트 ==========

    @Nested
    @DisplayName("배송지 단건 조회")
    class GetAddress {

        @Test
        @DisplayName("[성공] 배송지 단건 조회")
        void getAddress_success() {
            // given
            given(addressRepository.findById(ADDRESS_ID))
                .willReturn(Mono.just(testAddress));

            // when
            Mono<AddressResponse> result = addressService.getAddress(ADDRESS_ID, MEMBER_ID);

            // then
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getName()).isEqualTo("집");
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("[실패] 배송지 단건 조회 - 존재하지 않는 배송지")
        void getAddress_fail_notFound() {
            // given
            given(addressRepository.findById(999L))
                .willReturn(Mono.empty());

            // when
            Mono<AddressResponse> result = addressService.getAddress(999L, MEMBER_ID);

            // then
            StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                    throwable instanceof BusinessException &&
                    ((BusinessException) throwable).getErrorCode() == ErrorCode.ADDRESS_NOT_FOUND
                )
                .verify();
        }

        @Test
        @DisplayName("[실패] 배송지 단건 조회 - 다른 회원의 배송지")
        void getAddress_fail_unauthorized() {
            // given
            given(addressRepository.findById(ADDRESS_ID))
                .willReturn(Mono.just(testAddress));

            // when
            Mono<AddressResponse> result = addressService.getAddress(ADDRESS_ID, 999L);

            // then
            StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                    throwable instanceof BusinessException &&
                    ((BusinessException) throwable).getErrorCode() == ErrorCode.UNAUTHORIZED_ADDRESS_ACCESS
                )
                .verify();
        }
    }

    // ========== 기본 배송지 설정 테스트 ==========

    @Nested
    @DisplayName("기본 배송지 설정")
    class SetDefaultAddress {

        @Test
        @DisplayName("[성공] 기본 배송지 설정")
        void setDefaultAddress_success() {
            // given
            Address nonDefaultAddress = Address.builder()
                .id(ADDRESS_ID)
                .memberId(MEMBER_ID)
                .name("회사")
                .recipient("홍길동")
                .phone("01012345678")
                .zipCode("54321")
                .address("서울시 서초구")
                .isDefault(false)
                .build();

            given(addressRepository.findById(ADDRESS_ID))
                .willReturn(Mono.just(nonDefaultAddress));
            given(addressRepository.unsetDefaultByMemberId(MEMBER_ID))
                .willReturn(Mono.just(1));
            given(addressRepository.save(any(Address.class)))
                .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            // when
            Mono<AddressResponse> result = addressService.setDefaultAddress(ADDRESS_ID, MEMBER_ID);

            // then
            StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getIsDefault()).isTrue();
                })
                .verifyComplete();

            verify(addressRepository, times(1)).unsetDefaultByMemberId(MEMBER_ID);
        }

        @Test
        @DisplayName("[실패] 기본 배송지 설정 - 존재하지 않는 배송지")
        void setDefaultAddress_fail_notFound() {
            // given
            given(addressRepository.findById(999L))
                .willReturn(Mono.empty());

            // when
            Mono<AddressResponse> result = addressService.setDefaultAddress(999L, MEMBER_ID);

            // then
            StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                    throwable instanceof BusinessException &&
                    ((BusinessException) throwable).getErrorCode() == ErrorCode.ADDRESS_NOT_FOUND
                )
                .verify();
        }

        @Test
        @DisplayName("[실패] 기본 배송지 설정 - 다른 회원의 배송지")
        void setDefaultAddress_fail_unauthorized() {
            // given
            given(addressRepository.findById(ADDRESS_ID))
                .willReturn(Mono.just(testAddress));

            // when
            Mono<AddressResponse> result = addressService.setDefaultAddress(ADDRESS_ID, 999L);

            // then
            StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                    throwable instanceof BusinessException &&
                    ((BusinessException) throwable).getErrorCode() == ErrorCode.UNAUTHORIZED_ADDRESS_ACCESS
                )
                .verify();
        }
    }

    // ========== 배송지 삭제 테스트 ==========

    @Nested
    @DisplayName("배송지 삭제")
    class DeleteAddress {

        @Test
        @DisplayName("[성공] 배송지 삭제 - 기본 배송지가 아닌 경우")
        void deleteAddress_success() {
            // given
            Address nonDefaultAddress = Address.builder()
                .id(ADDRESS_ID)
                .memberId(MEMBER_ID)
                .name("회사")
                .recipient("홍길동")
                .phone("01012345678")
                .zipCode("54321")
                .address("서울시 서초구")
                .isDefault(false)
                .build();

            given(addressRepository.findById(ADDRESS_ID))
                .willReturn(Mono.just(nonDefaultAddress));
            given(addressRepository.deleteById(anyLong()))
                .willReturn(Mono.empty());

            // when
            Mono<Void> result = addressService.deleteAddress(ADDRESS_ID, MEMBER_ID);

            // then
            StepVerifier.create(result)
                .verifyComplete();

            verify(addressRepository, times(1)).deleteById(anyLong());
        }

        @Test
        @DisplayName("[성공] 배송지 삭제 - 유일한 기본 배송지인 경우")
        void deleteAddress_success_onlyDefaultAddress() {
            // given
            given(addressRepository.findById(ADDRESS_ID))
                .willReturn(Mono.just(testAddress));
            given(addressRepository.countByMemberId(MEMBER_ID))
                .willReturn(Mono.just(1L));
            given(addressRepository.deleteById(anyLong()))
                .willReturn(Mono.empty());

            // when
            Mono<Void> result = addressService.deleteAddress(ADDRESS_ID, MEMBER_ID);

            // then
            StepVerifier.create(result)
                .verifyComplete();
        }

        @Test
        @DisplayName("[실패] 배송지 삭제 - 존재하지 않는 배송지")
        void deleteAddress_fail_notFound() {
            // given
            given(addressRepository.findById(999L))
                .willReturn(Mono.empty());

            // when
            Mono<Void> result = addressService.deleteAddress(999L, MEMBER_ID);

            // then
            StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                    throwable instanceof BusinessException &&
                    ((BusinessException) throwable).getErrorCode() == ErrorCode.ADDRESS_NOT_FOUND
                )
                .verify();
        }

        @Test
        @DisplayName("[실패] 배송지 삭제 - 다른 회원의 배송지")
        void deleteAddress_fail_unauthorized() {
            // given
            given(addressRepository.findById(ADDRESS_ID))
                .willReturn(Mono.just(testAddress));

            // when
            Mono<Void> result = addressService.deleteAddress(ADDRESS_ID, 999L);

            // then
            StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                    throwable instanceof BusinessException &&
                    ((BusinessException) throwable).getErrorCode() == ErrorCode.UNAUTHORIZED_ADDRESS_ACCESS
                )
                .verify();
        }

        @Test
        @DisplayName("[실패] 배송지 삭제 - 기본 배송지이고 다른 배송지가 있는 경우")
        void deleteAddress_fail_cannotDeleteDefault() {
            // given
            given(addressRepository.findById(ADDRESS_ID))
                .willReturn(Mono.just(testAddress));
            given(addressRepository.countByMemberId(MEMBER_ID))
                .willReturn(Mono.just(2L));

            // when
            Mono<Void> result = addressService.deleteAddress(ADDRESS_ID, MEMBER_ID);

            // then
            StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                    throwable instanceof BusinessException &&
                    ((BusinessException) throwable).getErrorCode() == ErrorCode.CANNOT_DELETE_DEFAULT_ADDRESS
                )
                .verify();

            verify(addressRepository, times(0)).deleteById(anyLong());
        }

        @Test
        @DisplayName("[실패] 배송지 삭제 - DB 삭제 중 오류")
        void deleteAddress_fail_databaseError() {
            // given
            Address nonDefaultAddress = Address.builder()
                .id(ADDRESS_ID)
                .memberId(MEMBER_ID)
                .name("회사")
                .recipient("홍길동")
                .phone("01012345678")
                .zipCode("54321")
                .address("서울시 서초구")
                .isDefault(false)
                .build();

            given(addressRepository.findById(ADDRESS_ID))
                .willReturn(Mono.just(nonDefaultAddress));
            given(addressRepository.deleteById(anyLong()))
                .willReturn(Mono.error(new RuntimeException("Delete error")));

            // when
            Mono<Void> result = addressService.deleteAddress(ADDRESS_ID, MEMBER_ID);

            // then
            StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
        }
    }
}
