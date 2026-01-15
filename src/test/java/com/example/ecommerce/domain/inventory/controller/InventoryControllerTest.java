package com.example.ecommerce.domain.inventory.controller;

import com.example.ecommerce.domain.inventory.dto.request.*;
import com.example.ecommerce.domain.inventory.dto.response.InventoryHistoryResponse;
import com.example.ecommerce.domain.inventory.dto.response.InventoryResponse;
import com.example.ecommerce.domain.inventory.dto.response.InventoryStockResponse;
import com.example.ecommerce.domain.inventory.entity.InventoryChangeType;
import com.example.ecommerce.domain.inventory.service.InventoryService;
import com.example.ecommerce.global.common.PageResponse;
import com.example.ecommerce.global.config.RestDocsConfiguration;
import com.example.ecommerce.global.config.SecurityConfig;
import com.example.ecommerce.global.exception.BusinessException;
import com.example.ecommerce.global.exception.ErrorCode;
import com.example.ecommerce.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.documentationConfiguration;

@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@WebFluxTest(InventoryController.class)
@Import({RestDocsConfiguration.class, GlobalExceptionHandler.class, SecurityConfig.class})
@AutoConfigureRestDocs
@DisplayName("InventoryController 테스트")
class InventoryControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private InventoryService inventoryService;

    private InventoryResponse testInventoryResponse;
    private InventoryHistoryResponse testHistoryResponse;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        this.webTestClient = webTestClient.mutateWith(documentationConfiguration(restDocumentation)
                .operationPreprocessors()
                .withRequestDefaults(prettyPrint())
                .withResponseDefaults(prettyPrint()));

        testInventoryResponse = InventoryResponse.builder()
                .id(1L)
                .productOptionId(1L)
                .quantity(100)
                .reservedQuantity(10)
                .availableQuantity(90)
                .safetyStock(10)
                .isLowStock(false)
                .isSoldOut(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testHistoryResponse = InventoryHistoryResponse.builder()
                .id(1L)
                .inventoryId(1L)
                .productOptionId(1L)
                .changeType(InventoryChangeType.INCREASE)
                .changeTypeDescription("입고")
                .changeQuantity(50)
                .beforeQuantity(50)
                .afterQuantity(100)
                .reason("입고")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ========== 재고 생성 테스트 ==========

    @Nested
    @DisplayName("재고 생성")
    class CreateInventory {

        @Test
        @DisplayName("[성공] 재고 생성")
        void createInventory_success() {
            // given
            InventoryCreateRequest request = InventoryCreateRequest.builder()
                    .productOptionId(1L)
                    .quantity(100)
                    .safetyStock(10)
                    .build();

            given(inventoryService.createInventory(any(InventoryCreateRequest.class)))
                    .willReturn(Mono.just(testInventoryResponse));

            // when & then
            webTestClient.post()
                    .uri("/api/v1/inventories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data.id").isEqualTo(1)
                    .jsonPath("$.data.productOptionId").isEqualTo(1)
                    .jsonPath("$.data.quantity").isEqualTo(100)
                    .consumeWith(document("inventory/create-success",
                            requestFields(
                                    fieldWithPath("productOptionId").description("상품 옵션 ID"),
                                    fieldWithPath("quantity").description("초기 재고 수량"),
                                    fieldWithPath("safetyStock").description("안전 재고 수량").optional()
                            ),
                            responseFields(
                                    fieldWithPath("success").description("성공 여부"),
                                    fieldWithPath("message").description("응답 메시지"),
                                    fieldWithPath("data.id").description("재고 ID"),
                                    fieldWithPath("data.productOptionId").description("상품 옵션 ID"),
                                    fieldWithPath("data.quantity").description("현재 재고 수량"),
                                    fieldWithPath("data.reservedQuantity").description("예약된 재고 수량"),
                                    fieldWithPath("data.availableQuantity").description("가용 재고 수량"),
                                    fieldWithPath("data.safetyStock").description("안전 재고 수량"),
                                    fieldWithPath("data.isLowStock").description("안전 재고 이하 여부"),
                                    fieldWithPath("data.isSoldOut").description("품절 여부"),
                                    fieldWithPath("data.createdAt").description("생성일시"),
                                    fieldWithPath("data.updatedAt").description("수정일시")
                            )
                    ));
        }

        @Test
        @DisplayName("[실패] 재고 생성 - 존재하지 않는 상품 옵션")
        void createInventory_fail_optionNotFound() {
            // given
            InventoryCreateRequest request = InventoryCreateRequest.builder()
                    .productOptionId(999L)
                    .quantity(100)
                    .build();

            given(inventoryService.createInventory(any(InventoryCreateRequest.class)))
                    .willReturn(Mono.error(new BusinessException(ErrorCode.OPTION_NOT_FOUND)));

            // when & then
            webTestClient.post()
                    .uri("/api/v1/inventories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false);
        }

        @Test
        @DisplayName("[실패] 재고 생성 - 이미 등록된 재고")
        void createInventory_fail_duplicate() {
            // given
            InventoryCreateRequest request = InventoryCreateRequest.builder()
                    .productOptionId(1L)
                    .quantity(100)
                    .build();

            given(inventoryService.createInventory(any(InventoryCreateRequest.class)))
                    .willReturn(Mono.error(new BusinessException(ErrorCode.DUPLICATE_INVENTORY)));

            // when & then
            webTestClient.post()
                    .uri("/api/v1/inventories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().is4xxClientError()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false);
        }

        @Test
        @DisplayName("[실패] 재고 생성 - 필수값 누락")
        void createInventory_fail_invalidRequest() {
            // given
            InventoryCreateRequest request = InventoryCreateRequest.builder()
                    .quantity(100)
                    // productOptionId 누락
                    .build();

            // when & then
            webTestClient.post()
                    .uri("/api/v1/inventories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("[실패] 재고 생성 - 음수 수량")
        void createInventory_fail_negativeQuantity() {
            // given
            InventoryCreateRequest request = InventoryCreateRequest.builder()
                    .productOptionId(1L)
                    .quantity(-10)
                    .build();

            // when & then
            webTestClient.post()
                    .uri("/api/v1/inventories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest();
        }
    }

    // ========== 재고 조회 테스트 ==========

    @Nested
    @DisplayName("재고 조회")
    class GetInventory {

        @Test
        @DisplayName("[성공] 재고 상세 조회")
        void getInventory_success() {
            // given
            given(inventoryService.getInventory(1L))
                    .willReturn(Mono.just(testInventoryResponse));

            // when & then
            webTestClient.get()
                    .uri("/api/v1/inventories/{inventoryId}", 1L)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data.id").isEqualTo(1)
                    .consumeWith(document("inventory/get-success",
                            pathParameters(
                                    parameterWithName("inventoryId").description("재고 ID")
                            )
                    ));
        }

        @Test
        @DisplayName("[실패] 재고 상세 조회 - 존재하지 않는 재고")
        void getInventory_fail_notFound() {
            // given
            given(inventoryService.getInventory(999L))
                    .willReturn(Mono.error(new BusinessException(ErrorCode.INVENTORY_NOT_FOUND)));

            // when & then
            webTestClient.get()
                    .uri("/api/v1/inventories/{inventoryId}", 999L)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false);
        }
    }

    // ========== 재고 증가 테스트 ==========

    @Nested
    @DisplayName("재고 증가")
    class IncreaseStock {

        @Test
        @DisplayName("[성공] 재고 증가")
        void increaseStock_success() {
            // given
            InventoryIncreaseRequest request = InventoryIncreaseRequest.builder()
                    .quantity(50)
                    .reason("입고")
                    .build();

            InventoryResponse updatedResponse = InventoryResponse.builder()
                    .id(1L)
                    .productOptionId(1L)
                    .quantity(150)
                    .reservedQuantity(10)
                    .availableQuantity(140)
                    .safetyStock(10)
                    .isLowStock(false)
                    .isSoldOut(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            given(inventoryService.increaseStock(eq(1L), any(InventoryIncreaseRequest.class)))
                    .willReturn(Mono.just(updatedResponse));

            // when & then
            webTestClient.post()
                    .uri("/api/v1/inventories/{inventoryId}/increase", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data.quantity").isEqualTo(150)
                    .consumeWith(document("inventory/increase-success",
                            pathParameters(
                                    parameterWithName("inventoryId").description("재고 ID")
                            ),
                            requestFields(
                                    fieldWithPath("quantity").description("증가 수량"),
                                    fieldWithPath("reason").description("증가 사유").optional()
                            )
                    ));
        }

        @Test
        @DisplayName("[실패] 재고 증가 - 존재하지 않는 재고")
        void increaseStock_fail_notFound() {
            // given
            InventoryIncreaseRequest request = InventoryIncreaseRequest.builder()
                    .quantity(50)
                    .build();

            given(inventoryService.increaseStock(eq(999L), any(InventoryIncreaseRequest.class)))
                    .willReturn(Mono.error(new BusinessException(ErrorCode.INVENTORY_NOT_FOUND)));

            // when & then
            webTestClient.post()
                    .uri("/api/v1/inventories/{inventoryId}/increase", 999L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    // ========== 재고 예약 테스트 ==========

    @Nested
    @DisplayName("재고 예약")
    class ReserveStock {

        @Test
        @DisplayName("[성공] 재고 예약")
        void reserveStock_success() {
            // given
            InventoryReserveRequest request = InventoryReserveRequest.builder()
                    .quantity(20)
                    .orderId(100L)
                    .build();

            InventoryResponse reservedResponse = InventoryResponse.builder()
                    .id(1L)
                    .productOptionId(1L)
                    .quantity(100)
                    .reservedQuantity(30)
                    .availableQuantity(70)
                    .safetyStock(10)
                    .isLowStock(false)
                    .isSoldOut(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            given(inventoryService.reserveStock(eq(1L), any(InventoryReserveRequest.class)))
                    .willReturn(Mono.just(reservedResponse));

            // when & then
            webTestClient.post()
                    .uri("/api/v1/inventories/product-options/{productOptionId}/reserve", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data.reservedQuantity").isEqualTo(30)
                    .consumeWith(document("inventory/reserve-success",
                            pathParameters(
                                    parameterWithName("productOptionId").description("상품 옵션 ID")
                            ),
                            requestFields(
                                    fieldWithPath("quantity").description("예약 수량"),
                                    fieldWithPath("orderId").description("주문 ID").optional()
                            )
                    ));
        }

        @Test
        @DisplayName("[실패] 재고 예약 - 재고 부족")
        void reserveStock_fail_insufficientStock() {
            // given
            InventoryReserveRequest request = InventoryReserveRequest.builder()
                    .quantity(200)
                    .orderId(100L)
                    .build();

            given(inventoryService.reserveStock(eq(1L), any(InventoryReserveRequest.class)))
                    .willReturn(Mono.error(new BusinessException(ErrorCode.INSUFFICIENT_STOCK)));

            // when & then
            webTestClient.post()
                    .uri("/api/v1/inventories/product-options/{productOptionId}/reserve", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false);
        }

        @Test
        @DisplayName("[실패] 재고 예약 - 존재하지 않는 재고")
        void reserveStock_fail_notFound() {
            // given
            InventoryReserveRequest request = InventoryReserveRequest.builder()
                    .quantity(20)
                    .build();

            given(inventoryService.reserveStock(eq(999L), any(InventoryReserveRequest.class)))
                    .willReturn(Mono.error(new BusinessException(ErrorCode.INVENTORY_NOT_FOUND)));

            // when & then
            webTestClient.post()
                    .uri("/api/v1/inventories/product-options/{productOptionId}/reserve", 999L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    // ========== 재고 조정 테스트 ==========

    @Nested
    @DisplayName("재고 조정")
    class AdjustStock {

        @Test
        @DisplayName("[성공] 재고 조정")
        void adjustStock_success() {
            // given
            InventoryAdjustRequest request = InventoryAdjustRequest.builder()
                    .quantity(50)
                    .reason("재고 실사 조정")
                    .build();

            InventoryResponse adjustedResponse = InventoryResponse.builder()
                    .id(1L)
                    .productOptionId(1L)
                    .quantity(50)
                    .reservedQuantity(10)
                    .availableQuantity(40)
                    .safetyStock(10)
                    .isLowStock(false)
                    .isSoldOut(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            given(inventoryService.adjustStock(eq(1L), any(InventoryAdjustRequest.class)))
                    .willReturn(Mono.just(adjustedResponse));

            // when & then
            webTestClient.put()
                    .uri("/api/v1/inventories/{inventoryId}/adjust", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data.quantity").isEqualTo(50)
                    .consumeWith(document("inventory/adjust-success",
                            pathParameters(
                                    parameterWithName("inventoryId").description("재고 ID")
                            ),
                            requestFields(
                                    fieldWithPath("quantity").description("조정 수량"),
                                    fieldWithPath("reason").description("조정 사유")
                            )
                    ));
        }

        @Test
        @DisplayName("[실패] 재고 조정 - 필수값 누락 (사유)")
        void adjustStock_fail_noReason() {
            // given
            InventoryAdjustRequest request = InventoryAdjustRequest.builder()
                    .quantity(50)
                    // reason 누락
                    .build();

            // when & then
            webTestClient.put()
                    .uri("/api/v1/inventories/{inventoryId}/adjust", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("[실패] 재고 조정 - 존재하지 않는 재고")
        void adjustStock_fail_notFound() {
            // given
            InventoryAdjustRequest request = InventoryAdjustRequest.builder()
                    .quantity(50)
                    .reason("재고 조정")
                    .build();

            given(inventoryService.adjustStock(eq(999L), any(InventoryAdjustRequest.class)))
                    .willReturn(Mono.error(new BusinessException(ErrorCode.INVENTORY_NOT_FOUND)));

            // when & then
            webTestClient.put()
                    .uri("/api/v1/inventories/{inventoryId}/adjust", 999L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    // ========== 재고 이력 조회 테스트 ==========

    @Nested
    @DisplayName("재고 이력 조회")
    class GetInventoryHistories {

        @Test
        @DisplayName("[성공] 재고 이력 조회")
        void getInventoryHistories_success() {
            // given
            PageResponse<InventoryHistoryResponse> pageResponse = PageResponse.of(
                    List.of(testHistoryResponse),
                    1L,
                    0,
                    20
            );

            given(inventoryService.getInventoryHistories(eq(1L), anyInt(), anyInt()))
                    .willReturn(Mono.just(pageResponse));

            // when & then
            webTestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/inventories/{inventoryId}/histories")
                            .queryParam("page", 0)
                            .queryParam("size", 20)
                            .build(1L))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data.content").isArray()
                    .consumeWith(document("inventory/histories-success",
                            pathParameters(
                                    parameterWithName("inventoryId").description("재고 ID")
                            ),
                            queryParameters(
                                    parameterWithName("page").description("페이지 번호 (0부터 시작)"),
                                    parameterWithName("size").description("페이지 크기")
                            )
                    ));
        }
    }
}
