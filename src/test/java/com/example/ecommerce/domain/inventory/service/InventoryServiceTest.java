package com.example.ecommerce.domain.inventory.service;

import com.example.ecommerce.domain.inventory.dto.request.*;
import com.example.ecommerce.domain.inventory.dto.response.InventoryResponse;
import com.example.ecommerce.domain.inventory.entity.Inventory;
import com.example.ecommerce.domain.inventory.entity.InventoryHistory;
import com.example.ecommerce.domain.inventory.repository.InventoryHistoryRepository;
import com.example.ecommerce.domain.inventory.repository.InventoryRepository;
import com.example.ecommerce.domain.product.entity.ProductOption;
import com.example.ecommerce.domain.product.repository.ProductOptionRepository;
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

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService 테스트")
class InventoryServiceTest {

    @InjectMocks
    private InventoryService inventoryService;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryHistoryRepository historyRepository;

    @Mock
    private ProductOptionRepository productOptionRepository;

    private Inventory testInventory;
    private ProductOption testProductOption;

    @BeforeEach
    void setUp() throws Exception {
        testProductOption = ProductOption.builder()
                .productId(1L)
                .optionName("색상: 블랙")
                .option1("블랙")
                .addPrice(0)
                .stockQuantity(100)
                .isAvailable(true)
                .build();
        setId(testProductOption, 1L);

        testInventory = Inventory.builder()
                .productOptionId(1L)
                .quantity(100)
                .safetyStock(10)
                .build();
        setId(testInventory, 1L);
    }

    private void setId(Object entity, Long id) throws Exception {
        Field idField = entity.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    // ========== 재고 생성 테스트 ==========

    @Nested
    @DisplayName("재고 생성")
    class CreateInventory {

        @Test
        @DisplayName("[성공] 재고 생성 - 정상적으로 재고가 생성된다")
        void createInventory_success() {
            // given
            InventoryCreateRequest request = InventoryCreateRequest.builder()
                    .productOptionId(1L)
                    .quantity(100)
                    .safetyStock(10)
                    .build();

            given(productOptionRepository.findById(1L))
                    .willReturn(Mono.just(testProductOption));
            given(inventoryRepository.existsByProductOptionId(1L))
                    .willReturn(Mono.just(false));
            given(inventoryRepository.save(any(Inventory.class)))
                    .willReturn(Mono.just(testInventory));
            given(historyRepository.save(any(InventoryHistory.class)))
                    .willReturn(Mono.just(InventoryHistory.builder().build()));

            // when
            Mono<InventoryResponse> result = inventoryService.createInventory(request);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.getProductOptionId()).isEqualTo(1L);
                        assertThat(response.getQuantity()).isEqualTo(100);
                    })
                    .verifyComplete();

            verify(inventoryRepository, times(1)).save(any(Inventory.class));
        }

        @Test
        @DisplayName("[실패] 재고 생성 - 존재하지 않는 상품 옵션")
        void createInventory_fail_productOptionNotFound() {
            // given
            InventoryCreateRequest request = InventoryCreateRequest.builder()
                    .productOptionId(999L)
                    .quantity(100)
                    .build();

            given(productOptionRepository.findById(999L))
                    .willReturn(Mono.empty());

            // when
            Mono<InventoryResponse> result = inventoryService.createInventory(request);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.OPTION_NOT_FOUND
                    )
                    .verify();

            verify(inventoryRepository, never()).save(any(Inventory.class));
        }

        @Test
        @DisplayName("[실패] 재고 생성 - 이미 등록된 재고")
        void createInventory_fail_duplicateInventory() {
            // given
            InventoryCreateRequest request = InventoryCreateRequest.builder()
                    .productOptionId(1L)
                    .quantity(100)
                    .build();

            given(productOptionRepository.findById(1L))
                    .willReturn(Mono.just(testProductOption));
            given(inventoryRepository.existsByProductOptionId(1L))
                    .willReturn(Mono.just(true));

            // when
            Mono<InventoryResponse> result = inventoryService.createInventory(request);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.DUPLICATE_INVENTORY
                    )
                    .verify();

            verify(inventoryRepository, never()).save(any(Inventory.class));
        }

        @Test
        @DisplayName("[실패] 재고 생성 - DB 저장 실패")
        void createInventory_fail_databaseError() {
            // given
            InventoryCreateRequest request = InventoryCreateRequest.builder()
                    .productOptionId(1L)
                    .quantity(100)
                    .build();

            given(productOptionRepository.findById(1L))
                    .willReturn(Mono.just(testProductOption));
            given(inventoryRepository.existsByProductOptionId(1L))
                    .willReturn(Mono.just(false));
            given(inventoryRepository.save(any(Inventory.class)))
                    .willReturn(Mono.error(new RuntimeException("Database error")));

            // when
            Mono<InventoryResponse> result = inventoryService.createInventory(request);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.INTERNAL_SERVER_ERROR
                    )
                    .verify();
        }
    }

    // ========== 재고 조회 테스트 ==========

    @Nested
    @DisplayName("재고 조회")
    class GetInventory {

        @Test
        @DisplayName("[성공] 재고 조회 - 정상적으로 재고를 조회한다")
        void getInventory_success() {
            // given
            given(inventoryRepository.findById(1L))
                    .willReturn(Mono.just(testInventory));

            // when
            Mono<InventoryResponse> result = inventoryService.getInventory(1L);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.getId()).isEqualTo(1L);
                        assertThat(response.getQuantity()).isEqualTo(100);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("[실패] 재고 조회 - 존재하지 않는 재고")
        void getInventory_fail_notFound() {
            // given
            given(inventoryRepository.findById(999L))
                    .willReturn(Mono.empty());

            // when
            Mono<InventoryResponse> result = inventoryService.getInventory(999L);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.INVENTORY_NOT_FOUND
                    )
                    .verify();
        }
    }

    // ========== 재고 증가 테스트 ==========

    @Nested
    @DisplayName("재고 증가")
    class IncreaseStock {

        @Test
        @DisplayName("[성공] 재고 증가 - 정상적으로 재고가 증가한다")
        void increaseStock_success() throws Exception {
            // given
            InventoryIncreaseRequest request = InventoryIncreaseRequest.builder()
                    .quantity(50)
                    .reason("입고")
                    .build();

            Inventory updatedInventory = Inventory.builder()
                    .productOptionId(1L)
                    .quantity(150)
                    .safetyStock(10)
                    .build();
            setId(updatedInventory, 1L);

            given(inventoryRepository.findByIdWithLock(1L))
                    .willReturn(Mono.just(testInventory));
            given(inventoryRepository.save(any(Inventory.class)))
                    .willReturn(Mono.just(updatedInventory));
            given(historyRepository.save(any(InventoryHistory.class)))
                    .willReturn(Mono.just(InventoryHistory.builder().build()));

            // when
            Mono<InventoryResponse> result = inventoryService.increaseStock(1L, request);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.getQuantity()).isEqualTo(150);
                    })
                    .verifyComplete();

            verify(inventoryRepository, times(1)).save(any(Inventory.class));
            verify(historyRepository, times(1)).save(any(InventoryHistory.class));
        }

        @Test
        @DisplayName("[실패] 재고 증가 - 존재하지 않는 재고")
        void increaseStock_fail_notFound() {
            // given
            InventoryIncreaseRequest request = InventoryIncreaseRequest.builder()
                    .quantity(50)
                    .build();

            given(inventoryRepository.findByIdWithLock(999L))
                    .willReturn(Mono.empty());

            // when
            Mono<InventoryResponse> result = inventoryService.increaseStock(999L, request);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.INVENTORY_NOT_FOUND
                    )
                    .verify();

            verify(inventoryRepository, never()).save(any(Inventory.class));
        }
    }

    // ========== 재고 감소 테스트 ==========

    @Nested
    @DisplayName("재고 감소")
    class DecreaseStock {

        @Test
        @DisplayName("[성공] 재고 감소 - 정상적으로 재고가 감소한다")
        void decreaseStock_success() throws Exception {
            // given
            InventoryDecreaseRequest request = InventoryDecreaseRequest.builder()
                    .quantity(30)
                    .reason("출고")
                    .build();

            Inventory updatedInventory = Inventory.builder()
                    .productOptionId(1L)
                    .quantity(70)
                    .safetyStock(10)
                    .build();
            setId(updatedInventory, 1L);

            given(inventoryRepository.findByIdWithLock(1L))
                    .willReturn(Mono.just(testInventory));
            given(inventoryRepository.save(any(Inventory.class)))
                    .willReturn(Mono.just(updatedInventory));
            given(historyRepository.save(any(InventoryHistory.class)))
                    .willReturn(Mono.just(InventoryHistory.builder().build()));

            // when
            Mono<InventoryResponse> result = inventoryService.decreaseStock(1L, request);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.getQuantity()).isEqualTo(70);
                    })
                    .verifyComplete();

            verify(inventoryRepository, times(1)).save(any(Inventory.class));
        }

        @Test
        @DisplayName("[실패] 재고 감소 - 재고 부족")
        void decreaseStock_fail_insufficientStock() {
            // given
            InventoryDecreaseRequest request = InventoryDecreaseRequest.builder()
                    .quantity(200) // 현재 재고보다 많음
                    .build();

            given(inventoryRepository.findByIdWithLock(1L))
                    .willReturn(Mono.just(testInventory));

            // when
            Mono<InventoryResponse> result = inventoryService.decreaseStock(1L, request);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.INSUFFICIENT_STOCK
                    )
                    .verify();

            verify(inventoryRepository, never()).save(any(Inventory.class));
        }

        @Test
        @DisplayName("[실패] 재고 감소 - 존재하지 않는 재고")
        void decreaseStock_fail_notFound() {
            // given
            InventoryDecreaseRequest request = InventoryDecreaseRequest.builder()
                    .quantity(30)
                    .build();

            given(inventoryRepository.findByIdWithLock(999L))
                    .willReturn(Mono.empty());

            // when
            Mono<InventoryResponse> result = inventoryService.decreaseStock(999L, request);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.INVENTORY_NOT_FOUND
                    )
                    .verify();
        }
    }

    // ========== 재고 예약 테스트 ==========

    @Nested
    @DisplayName("재고 예약")
    class ReserveStock {

        @Test
        @DisplayName("[성공] 재고 예약 - 정상적으로 재고가 예약된다")
        void reserveStock_success() throws Exception {
            // given
            InventoryReserveRequest request = InventoryReserveRequest.builder()
                    .quantity(20)
                    .orderId(100L)
                    .build();

            Inventory reservedInventory = Inventory.builder()
                    .productOptionId(1L)
                    .quantity(100)
                    .safetyStock(10)
                    .build();
            setId(reservedInventory, 1L);
            // reserved quantity는 20이 되어야 함

            given(inventoryRepository.findByProductOptionIdWithLock(1L))
                    .willReturn(Mono.just(testInventory));
            given(inventoryRepository.save(any(Inventory.class)))
                    .willReturn(Mono.just(reservedInventory));
            given(historyRepository.save(any(InventoryHistory.class)))
                    .willReturn(Mono.just(InventoryHistory.builder().build()));

            // when
            Mono<InventoryResponse> result = inventoryService.reserveStock(1L, request);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.getProductOptionId()).isEqualTo(1L);
                    })
                    .verifyComplete();

            verify(inventoryRepository, times(1)).save(any(Inventory.class));
            verify(historyRepository, times(1)).save(any(InventoryHistory.class));
        }

        @Test
        @DisplayName("[실패] 재고 예약 - 가용 재고 부족")
        void reserveStock_fail_insufficientStock() {
            // given
            InventoryReserveRequest request = InventoryReserveRequest.builder()
                    .quantity(200) // 가용 재고보다 많음
                    .orderId(100L)
                    .build();

            given(inventoryRepository.findByProductOptionIdWithLock(1L))
                    .willReturn(Mono.just(testInventory));

            // when
            Mono<InventoryResponse> result = inventoryService.reserveStock(1L, request);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.INSUFFICIENT_STOCK
                    )
                    .verify();

            verify(inventoryRepository, never()).save(any(Inventory.class));
        }

        @Test
        @DisplayName("[실패] 재고 예약 - 존재하지 않는 재고")
        void reserveStock_fail_notFound() {
            // given
            InventoryReserveRequest request = InventoryReserveRequest.builder()
                    .quantity(20)
                    .orderId(100L)
                    .build();

            given(inventoryRepository.findByProductOptionIdWithLock(999L))
                    .willReturn(Mono.empty());

            // when
            Mono<InventoryResponse> result = inventoryService.reserveStock(999L, request);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.INVENTORY_NOT_FOUND
                    )
                    .verify();
        }
    }

    // ========== 예약 해제 테스트 ==========

    @Nested
    @DisplayName("예약 해제")
    class ReleaseStock {

        @Test
        @DisplayName("[성공] 예약 해제 - 정상적으로 예약이 해제된다")
        void releaseStock_success() throws Exception {
            // given
            Inventory inventoryWithReservation = Inventory.builder()
                    .productOptionId(1L)
                    .quantity(100)
                    .safetyStock(10)
                    .build();
            setId(inventoryWithReservation, 1L);
            // 예약 수량 설정을 위해 reserve 호출
            inventoryWithReservation.reserve(30);

            given(inventoryRepository.findByProductOptionIdWithLock(1L))
                    .willReturn(Mono.just(inventoryWithReservation));
            given(inventoryRepository.save(any(Inventory.class)))
                    .willReturn(Mono.just(inventoryWithReservation));
            given(historyRepository.save(any(InventoryHistory.class)))
                    .willReturn(Mono.just(InventoryHistory.builder().build()));

            // when
            Mono<InventoryResponse> result = inventoryService.releaseStock(1L, 20, 100L, "주문 취소");

            // then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                    })
                    .verifyComplete();

            verify(inventoryRepository, times(1)).save(any(Inventory.class));
        }

        @Test
        @DisplayName("[실패] 예약 해제 - 예약 수량보다 많은 해제 요청")
        void releaseStock_fail_invalidOperation() throws Exception {
            // given
            Inventory inventoryWithReservation = Inventory.builder()
                    .productOptionId(1L)
                    .quantity(100)
                    .safetyStock(10)
                    .build();
            setId(inventoryWithReservation, 1L);
            inventoryWithReservation.reserve(10);

            given(inventoryRepository.findByProductOptionIdWithLock(1L))
                    .willReturn(Mono.just(inventoryWithReservation));

            // when
            Mono<InventoryResponse> result = inventoryService.releaseStock(1L, 50, 100L, "주문 취소"); // 예약 수량보다 많음

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.INVALID_INVENTORY_OPERATION
                    )
                    .verify();

            verify(inventoryRepository, never()).save(any(Inventory.class));
        }

        @Test
        @DisplayName("[실패] 예약 해제 - 존재하지 않는 재고")
        void releaseStock_fail_notFound() {
            // given
            given(inventoryRepository.findByProductOptionIdWithLock(999L))
                    .willReturn(Mono.empty());

            // when
            Mono<InventoryResponse> result = inventoryService.releaseStock(999L, 20, 100L, "주문 취소");

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.INVENTORY_NOT_FOUND
                    )
                    .verify();
        }
    }

    // ========== 재고 조정 테스트 ==========

    @Nested
    @DisplayName("재고 조정")
    class AdjustStock {

        @Test
        @DisplayName("[성공] 재고 조정 - 정상적으로 재고가 조정된다")
        void adjustStock_success() throws Exception {
            // given
            InventoryAdjustRequest request = InventoryAdjustRequest.builder()
                    .quantity(50)
                    .reason("재고 실사 조정")
                    .build();

            Inventory adjustedInventory = Inventory.builder()
                    .productOptionId(1L)
                    .quantity(50)
                    .safetyStock(10)
                    .build();
            setId(adjustedInventory, 1L);

            given(inventoryRepository.findByIdWithLock(1L))
                    .willReturn(Mono.just(testInventory));
            given(inventoryRepository.save(any(Inventory.class)))
                    .willReturn(Mono.just(adjustedInventory));
            given(historyRepository.save(any(InventoryHistory.class)))
                    .willReturn(Mono.just(InventoryHistory.builder().build()));

            // when
            Mono<InventoryResponse> result = inventoryService.adjustStock(1L, request);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.getQuantity()).isEqualTo(50);
                    })
                    .verifyComplete();

            verify(inventoryRepository, times(1)).save(any(Inventory.class));
        }

        @Test
        @DisplayName("[실패] 재고 조정 - 음수 수량")
        void adjustStock_fail_negativeQuantity() {
            // given
            InventoryAdjustRequest request = InventoryAdjustRequest.builder()
                    .quantity(-10)
                    .reason("재고 조정")
                    .build();

            given(inventoryRepository.findByIdWithLock(1L))
                    .willReturn(Mono.just(testInventory));

            // when
            Mono<InventoryResponse> result = inventoryService.adjustStock(1L, request);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.INVALID_INVENTORY_QUANTITY
                    )
                    .verify();

            verify(inventoryRepository, never()).save(any(Inventory.class));
        }

        @Test
        @DisplayName("[실패] 재고 조정 - 존재하지 않는 재고")
        void adjustStock_fail_notFound() {
            // given
            InventoryAdjustRequest request = InventoryAdjustRequest.builder()
                    .quantity(50)
                    .reason("재고 조정")
                    .build();

            given(inventoryRepository.findByIdWithLock(999L))
                    .willReturn(Mono.empty());

            // when
            Mono<InventoryResponse> result = inventoryService.adjustStock(999L, request);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.INVENTORY_NOT_FOUND
                    )
                    .verify();
        }

        @Test
        @DisplayName("[실패] 재고 조정 - 예약 수량보다 적은 수량으로 조정")
        void adjustStock_fail_lessThanReserved() throws Exception {
            // given
            Inventory inventoryWithReservation = Inventory.builder()
                    .productOptionId(1L)
                    .quantity(100)
                    .safetyStock(10)
                    .build();
            setId(inventoryWithReservation, 1L);
            inventoryWithReservation.reserve(50);

            InventoryAdjustRequest request = InventoryAdjustRequest.builder()
                    .quantity(30) // 예약 수량(50)보다 적음
                    .reason("재고 조정")
                    .build();

            given(inventoryRepository.findByIdWithLock(1L))
                    .willReturn(Mono.just(inventoryWithReservation));

            // when
            Mono<InventoryResponse> result = inventoryService.adjustStock(1L, request);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.INVALID_INVENTORY_OPERATION
                    )
                    .verify();

            verify(inventoryRepository, never()).save(any(Inventory.class));
        }
    }
}
