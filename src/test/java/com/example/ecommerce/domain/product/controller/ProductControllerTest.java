package com.example.ecommerce.domain.product.controller;

import com.example.ecommerce.domain.product.dto.request.ProductCreateRequest;
import com.example.ecommerce.domain.product.dto.request.ProductImageRequest;
import com.example.ecommerce.domain.product.dto.request.ProductOptionRequest;
import com.example.ecommerce.domain.product.dto.request.ProductSearchRequest;
import com.example.ecommerce.domain.product.dto.request.ProductStatusUpdateRequest;
import com.example.ecommerce.domain.product.dto.request.ProductUpdateRequest;
import com.example.ecommerce.domain.product.dto.response.CategoryResponse;
import com.example.ecommerce.domain.product.dto.response.ProductDetailResponse;
import com.example.ecommerce.domain.product.dto.response.ProductListResponse;
import com.example.ecommerce.domain.product.dto.response.ProductResponse;
import com.example.ecommerce.domain.product.entity.DeliveryType;
import com.example.ecommerce.domain.product.entity.ProductStatus;
import com.example.ecommerce.domain.product.service.ProductService;
import com.example.ecommerce.global.common.PageResponse;
import com.example.ecommerce.global.config.RestDocsConfiguration;
import com.example.ecommerce.global.exception.BusinessException;
import com.example.ecommerce.global.exception.ErrorCode;
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
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@WebFluxTest(ProductController.class)
@AutoConfigureRestDocs
@Import(RestDocsConfiguration.class)
@DisplayName("ProductController 테스트")
class ProductControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ProductService productService;

    // ========== 상품 목록 조회 테스트 ==========

    @Nested
    @DisplayName("상품 목록 조회 (GET /api/v1/products)")
    class GetProducts {

        @Test
        @DisplayName("[성공] 카테고리별 상품 목록 조회")
        void getProducts_success() {
            // given
            ProductListResponse product1 = ProductListResponse.builder()
                    .id(1L)
                    .name("테스트 상품 1")
                    .basePrice(50000)
                    .discountRate(10)
                    .discountPrice(45000)
                    .formattedDiscountPrice("45,000원")
                    .deliveryType(DeliveryType.ROCKET)
                    .deliveryTypeDisplayName("로켓배송")
                    .averageRating(4.5)
                    .reviewCount(100)
                    .thumbnailUrl("https://example.com/image1.jpg")
                    .isSoldOut(false)
                    .isRocketDelivery(true)
                    .build();

            ProductListResponse product2 = ProductListResponse.builder()
                    .id(2L)
                    .name("테스트 상품 2")
                    .basePrice(30000)
                    .discountRate(0)
                    .discountPrice(30000)
                    .formattedDiscountPrice("30,000원")
                    .deliveryType(DeliveryType.NORMAL)
                    .deliveryTypeDisplayName("일반배송")
                    .averageRating(4.0)
                    .reviewCount(50)
                    .thumbnailUrl("https://example.com/image2.jpg")
                    .isSoldOut(false)
                    .isRocketDelivery(false)
                    .build();

            PageResponse<ProductListResponse> pageResponse = PageResponse.of(
                    List.of(product1, product2), 2L, 1, 20
            );

            given(productService.searchProducts(any(ProductSearchRequest.class)))
                    .willReturn(Mono.just(pageResponse));

            // when & then
            webTestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/products")
                            .queryParam("categoryId", 1)
                            .queryParam("sortType", "POPULAR")
                            .queryParam("page", 1)
                            .queryParam("size", 20)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.result").isEqualTo(true)
                    .jsonPath("$.data.content").isArray()
                    .jsonPath("$.data.content[0].id").isEqualTo(1)
                    .jsonPath("$.data.content[0].name").isEqualTo("테스트 상품 1")
                    .jsonPath("$.data.totalElements").isEqualTo(2)
                    .consumeWith(document("products/list-success",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint())
                    ));
        }

        @Test
        @DisplayName("[실패] 잘못된 페이지 번호")
        void getProducts_fail_invalidPage() {
            // when & then
            webTestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/products")
                            .queryParam("categoryId", 1)
                            .queryParam("page", 0) // 잘못된 페이지
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("[실패] 잘못된 페이지 크기 (100 초과)")
        void getProducts_fail_invalidSize() {
            // when & then
            webTestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/products")
                            .queryParam("categoryId", 1)
                            .queryParam("size", 101) // 100 초과
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isBadRequest();
        }
    }

    // ========== 상품 상세 조회 테스트 ==========

    @Nested
    @DisplayName("상품 상세 조회 (GET /api/v1/products/{id})")
    class GetProduct {

        @Test
        @DisplayName("[성공] 상품 상세 정보 조회")
        void getProduct_success() {
            // given
            ProductDetailResponse response = ProductDetailResponse.builder()
                    .id(1L)
                    .sellerId(1L)
                    .category(CategoryResponse.builder()
                            .id(1L)
                            .name("패션의류")
                            .depth(0)
                            .build())
                    .name("테스트 상품")
                    .description("상품 설명입니다.")
                    .basePrice(50000)
                    .discountRate(10)
                    .discountPrice(45000)
                    .formattedBasePrice("50,000원")
                    .formattedDiscountPrice("45,000원")
                    .status(ProductStatus.ON_SALE)
                    .statusDisplayName("판매중")
                    .deliveryType(DeliveryType.ROCKET)
                    .deliveryTypeDisplayName("로켓배송")
                    .deliveryFee(0)
                    .freeDeliveryThreshold(19800)
                    .averageRating(4.5)
                    .reviewCount(100)
                    .salesCount(500)
                    .viewCount(1000)
                    .options(List.of())
                    .images(List.of())
                    .thumbnailUrl("https://example.com/image.jpg")
                    .isAvailable(true)
                    .isRocketDelivery(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            given(productService.getProductDetail(1L))
                    .willReturn(Mono.just(response));

            // when & then
            webTestClient.get()
                    .uri("/api/v1/products/1")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.result").isEqualTo(true)
                    .jsonPath("$.data.id").isEqualTo(1)
                    .jsonPath("$.data.name").isEqualTo("테스트 상품")
                    .jsonPath("$.data.status").isEqualTo("ON_SALE")
                    .consumeWith(document("products/get-success",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint())
                    ));
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 상품")
        void getProduct_fail_notFound() {
            // given
            given(productService.getProductDetail(999L))
                    .willReturn(Mono.error(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)));

            // when & then
            webTestClient.get()
                    .uri("/api/v1/products/999")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.result").isEqualTo(false)
                    .jsonPath("$.error.code").isEqualTo("P001");
        }

        @Test
        @DisplayName("[실패] 삭제된 상품 조회")
        void getProduct_fail_deleted() {
            // given
            given(productService.getProductDetail(2L))
                    .willReturn(Mono.error(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)));

            // when & then
            webTestClient.get()
                    .uri("/api/v1/products/2")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    // ========== 상품 등록 테스트 ==========

    @Nested
    @DisplayName("상품 등록 (POST /api/v1/products)")
    class CreateProduct {

        @Test
        @DisplayName("[성공] 상품 등록")
        void createProduct_success() {
            // given
            ProductCreateRequest request = ProductCreateRequest.builder()
                    .categoryId(1L)
                    .name("새 상품")
                    .description("상품 설명")
                    .basePrice(50000)
                    .discountRate(10)
                    .deliveryType(DeliveryType.ROCKET)
                    .options(List.of(
                            ProductOptionRequest.builder()
                                    .optionName("색상: 블랙")
                                    .option1("블랙")
                                    .addPrice(0)
                                    .stockQuantity(100)
                                    .build()
                    ))
                    .images(List.of(
                            ProductImageRequest.builder()
                                    .imageUrl("https://example.com/image.jpg")
                                    .sortOrder(0)
                                    .isThumbnail(true)
                                    .build()
                    ))
                    .build();

            ProductDetailResponse response = ProductDetailResponse.builder()
                    .id(1L)
                    .sellerId(1L)
                    .name("새 상품")
                    .basePrice(50000)
                    .discountPrice(45000)
                    .status(ProductStatus.ON_SALE)
                    .build();

            given(productService.createProduct(eq(1L), any(ProductCreateRequest.class)))
                    .willReturn(Mono.just(response));

            // when & then
            webTestClient.post()
                    .uri("/api/v1/products")
                    .attribute("memberId", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.result").isEqualTo(true)
                    .jsonPath("$.data.id").isEqualTo(1)
                    .consumeWith(document("products/create-success",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint())
                    ));
        }

        @Test
        @DisplayName("[실패] 필수 필드 누락 - 상품명")
        void createProduct_fail_missingName() {
            // given
            ProductCreateRequest request = ProductCreateRequest.builder()
                    .categoryId(1L)
                    // name 누락
                    .basePrice(50000)
                    .options(List.of(
                            ProductOptionRequest.builder()
                                    .optionName("옵션")
                                    .stockQuantity(100)
                                    .build()
                    ))
                    .images(List.of(
                            ProductImageRequest.builder()
                                    .imageUrl("https://example.com/image.jpg")
                                    .build()
                    ))
                    .build();

            // when & then
            webTestClient.post()
                    .uri("/api/v1/products")
                    .attribute("memberId", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("[실패] 유효하지 않은 가격 (음수)")
        void createProduct_fail_invalidPrice() {
            // given
            ProductCreateRequest request = ProductCreateRequest.builder()
                    .categoryId(1L)
                    .name("상품")
                    .basePrice(-1000) // 음수 가격
                    .options(List.of(
                            ProductOptionRequest.builder()
                                    .optionName("옵션")
                                    .stockQuantity(100)
                                    .build()
                    ))
                    .images(List.of(
                            ProductImageRequest.builder()
                                    .imageUrl("https://example.com/image.jpg")
                                    .build()
                    ))
                    .build();

            // when & then
            webTestClient.post()
                    .uri("/api/v1/products")
                    .attribute("memberId", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 카테고리")
        void createProduct_fail_categoryNotFound() {
            // given
            ProductCreateRequest request = ProductCreateRequest.builder()
                    .categoryId(999L) // 존재하지 않는 카테고리
                    .name("상품")
                    .basePrice(50000)
                    .options(List.of(
                            ProductOptionRequest.builder()
                                    .optionName("옵션")
                                    .stockQuantity(100)
                                    .build()
                    ))
                    .images(List.of(
                            ProductImageRequest.builder()
                                    .imageUrl("https://example.com/image.jpg")
                                    .build()
                    ))
                    .build();

            given(productService.createProduct(eq(1L), any(ProductCreateRequest.class)))
                    .willReturn(Mono.error(new BusinessException(ErrorCode.CATEGORY_NOT_FOUND)));

            // when & then
            webTestClient.post()
                    .uri("/api/v1/products")
                    .attribute("memberId", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.error.code").isEqualTo("P004");
        }

        @Test
        @DisplayName("[실패] 옵션 없이 등록")
        void createProduct_fail_noOptions() {
            // given
            ProductCreateRequest request = ProductCreateRequest.builder()
                    .categoryId(1L)
                    .name("상품")
                    .basePrice(50000)
                    .options(List.of()) // 빈 옵션
                    .images(List.of(
                            ProductImageRequest.builder()
                                    .imageUrl("https://example.com/image.jpg")
                                    .build()
                    ))
                    .build();

            // when & then
            webTestClient.post()
                    .uri("/api/v1/products")
                    .attribute("memberId", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("[실패] 이미지 없이 등록")
        void createProduct_fail_noImages() {
            // given
            ProductCreateRequest request = ProductCreateRequest.builder()
                    .categoryId(1L)
                    .name("상품")
                    .basePrice(50000)
                    .options(List.of(
                            ProductOptionRequest.builder()
                                    .optionName("옵션")
                                    .stockQuantity(100)
                                    .build()
                    ))
                    .images(List.of()) // 빈 이미지
                    .build();

            // when & then
            webTestClient.post()
                    .uri("/api/v1/products")
                    .attribute("memberId", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest();
        }
    }

    // ========== 상품 수정 테스트 ==========

    @Nested
    @DisplayName("상품 수정 (PUT /api/v1/products/{id})")
    class UpdateProduct {

        @Test
        @DisplayName("[성공] 상품 정보 수정")
        void updateProduct_success() {
            // given
            ProductUpdateRequest request = ProductUpdateRequest.builder()
                    .name("수정된 상품명")
                    .description("수정된 설명")
                    .basePrice(60000)
                    .build();

            ProductDetailResponse response = ProductDetailResponse.builder()
                    .id(1L)
                    .name("수정된 상품명")
                    .basePrice(60000)
                    .status(ProductStatus.ON_SALE)
                    .build();

            given(productService.updateProduct(eq(1L), eq(1L), any(ProductUpdateRequest.class)))
                    .willReturn(Mono.just(response));

            // when & then
            webTestClient.put()
                    .uri("/api/v1/products/1")
                    .attribute("memberId", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.result").isEqualTo(true)
                    .jsonPath("$.data.name").isEqualTo("수정된 상품명")
                    .consumeWith(document("products/update-success",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint())
                    ));
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 상품")
        void updateProduct_fail_notFound() {
            // given
            ProductUpdateRequest request = ProductUpdateRequest.builder()
                    .name("수정")
                    .build();

            given(productService.updateProduct(eq(1L), eq(999L), any(ProductUpdateRequest.class)))
                    .willReturn(Mono.error(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)));

            // when & then
            webTestClient.put()
                    .uri("/api/v1/products/999")
                    .attribute("memberId", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        @DisplayName("[실패] 권한 없음 (다른 판매자의 상품)")
        void updateProduct_fail_accessDenied() {
            // given
            ProductUpdateRequest request = ProductUpdateRequest.builder()
                    .name("수정")
                    .build();

            given(productService.updateProduct(eq(2L), eq(1L), any(ProductUpdateRequest.class)))
                    .willReturn(Mono.error(new BusinessException(ErrorCode.ACCESS_DENIED)));

            // when & then
            webTestClient.put()
                    .uri("/api/v1/products/1")
                    .attribute("memberId", 2L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isForbidden();
        }

        @Test
        @DisplayName("[실패] 유효하지 않은 할인율 (100 초과)")
        void updateProduct_fail_invalidDiscountRate() {
            // given
            ProductUpdateRequest request = ProductUpdateRequest.builder()
                    .discountRate(150) // 100 초과
                    .build();

            // when & then
            webTestClient.put()
                    .uri("/api/v1/products/1")
                    .attribute("memberId", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest();
        }
    }

    // ========== 상품 삭제 테스트 ==========

    @Nested
    @DisplayName("상품 삭제 (DELETE /api/v1/products/{id})")
    class DeleteProduct {

        @Test
        @DisplayName("[성공] 상품 삭제")
        void deleteProduct_success() {
            // given
            given(productService.deleteProduct(1L, 1L))
                    .willReturn(Mono.empty());

            // when & then
            webTestClient.delete()
                    .uri("/api/v1/products/1")
                    .attribute("memberId", 1L)
                    .exchange()
                    .expectStatus().isNoContent()
                    .consumeWith(document("products/delete-success",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint())
                    ));
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 상품")
        void deleteProduct_fail_notFound() {
            // given
            given(productService.deleteProduct(1L, 999L))
                    .willReturn(Mono.error(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)));

            // when & then
            webTestClient.delete()
                    .uri("/api/v1/products/999")
                    .attribute("memberId", 1L)
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        @DisplayName("[실패] 권한 없음")
        void deleteProduct_fail_accessDenied() {
            // given
            given(productService.deleteProduct(2L, 1L))
                    .willReturn(Mono.error(new BusinessException(ErrorCode.ACCESS_DENIED)));

            // when & then
            webTestClient.delete()
                    .uri("/api/v1/products/1")
                    .attribute("memberId", 2L)
                    .exchange()
                    .expectStatus().isForbidden();
        }

        @Test
        @DisplayName("[실패] 이미 삭제된 상품")
        void deleteProduct_fail_alreadyDeleted() {
            // given
            given(productService.deleteProduct(1L, 1L))
                    .willReturn(Mono.error(new BusinessException(ErrorCode.PRODUCT_ALREADY_DELETED)));

            // when & then
            webTestClient.delete()
                    .uri("/api/v1/products/1")
                    .attribute("memberId", 1L)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.error.code").isEqualTo("P009");
        }
    }

    // ========== 상품 상태 변경 테스트 ==========

    @Nested
    @DisplayName("상품 상태 변경 (PATCH /api/v1/products/{id}/status)")
    class UpdateProductStatus {

        @Test
        @DisplayName("[성공] 상품 판매 중지")
        void updateProductStatus_success() {
            // given
            ProductStatusUpdateRequest request = ProductStatusUpdateRequest.builder()
                    .status(ProductStatus.STOP_SALE)
                    .build();

            ProductResponse response = ProductResponse.builder()
                    .id(1L)
                    .status(ProductStatus.STOP_SALE)
                    .statusDisplayName("판매중지")
                    .build();

            given(productService.updateProductStatus(eq(1L), eq(1L), any(ProductStatusUpdateRequest.class)))
                    .willReturn(Mono.just(response));

            // when & then
            webTestClient.patch()
                    .uri("/api/v1/products/1/status")
                    .attribute("memberId", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.result").isEqualTo(true)
                    .jsonPath("$.data.status").isEqualTo("STOP_SALE")
                    .consumeWith(document("products/update-status-success",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint())
                    ));
        }
    }
}
