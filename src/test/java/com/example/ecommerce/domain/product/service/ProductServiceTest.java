package com.example.ecommerce.domain.product.service;

import com.example.ecommerce.domain.product.dto.request.ProductCreateRequest;
import com.example.ecommerce.domain.product.dto.request.ProductImageRequest;
import com.example.ecommerce.domain.product.dto.request.ProductOptionRequest;
import com.example.ecommerce.domain.product.dto.request.ProductSearchRequest;
import com.example.ecommerce.domain.product.dto.request.ProductStatusUpdateRequest;
import com.example.ecommerce.domain.product.dto.request.ProductUpdateRequest;
import com.example.ecommerce.domain.product.dto.response.ProductDetailResponse;
import com.example.ecommerce.domain.product.dto.response.ProductResponse;
import com.example.ecommerce.domain.product.entity.Category;
import com.example.ecommerce.domain.product.entity.DeliveryType;
import com.example.ecommerce.domain.product.entity.ImageType;
import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.product.entity.ProductImage;
import com.example.ecommerce.domain.product.entity.ProductOption;
import com.example.ecommerce.domain.product.entity.ProductStatus;
import com.example.ecommerce.domain.product.repository.CategoryRepository;
import com.example.ecommerce.domain.product.repository.ProductImageRepository;
import com.example.ecommerce.domain.product.repository.ProductOptionRepository;
import com.example.ecommerce.domain.product.repository.ProductRepository;
import com.example.ecommerce.domain.seller.entity.Seller;
import com.example.ecommerce.domain.seller.entity.SellerStatus;
import com.example.ecommerce.domain.seller.service.SellerService;
import com.example.ecommerce.global.common.PageResponse;
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

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 테스트")
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductOptionRepository productOptionRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SellerService sellerService;

    private Seller testSeller;
    private Product testProduct;
    private Category testCategory;
    private ProductOption testOption;
    private ProductImage testImage;
    private ProductCreateRequest createRequest;
    private ProductUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        testSeller = Seller.builder()
                .id(1L)
                .memberId(100L)
                .businessName("테스트 판매자")
                .businessNumber("123-45-67890")
                .representativeName("홍길동")
                .status(SellerStatus.ACTIVE)
                .build();

        testCategory = Category.builder()
                .id(10L)
                .name("전자기기")
                .depth(0)
                .sortOrder(1)
                .isActive(true)
                .build();

        testProduct = Product.builder()
                .id(1L)
                .sellerId(1L)
                .categoryId(10L)
                .name("테스트 상품")
                .description("테스트 상품 설명")
                .basePrice(new BigDecimal("50000"))
                .discountRate(10)
                .sellingPrice(new BigDecimal("45000"))
                .status(ProductStatus.ON_SALE)
                .deliveryType(DeliveryType.ROCKET)
                .viewCount(0L)
                .salesCount(0L)
                .reviewCount(0L)
                .averageRating(0.0)
                .build();

        testOption = ProductOption.builder()
                .id(1L)
                .productId(1L)
                .optionName("색상")
                .option1("블랙")
                .option2(null)
                .additionalPrice(BigDecimal.ZERO)
                .stockQuantity(100)
                .isAvailable(true)
                .build();

        testImage = ProductImage.builder()
                .id(1L)
                .productId(1L)
                .imageUrl("https://example.com/image1.jpg")
                .sortOrder(1)
                .isThumbnail(true)
                .imageType(ImageType.MAIN)
                .build();

        ProductOptionRequest optionRequest = ProductOptionRequest.builder()
                .optionName("색상")
                .option1("블랙")
                .additionalPrice(BigDecimal.ZERO)
                .stockQuantity(100)
                .build();

        ProductImageRequest imageRequest = ProductImageRequest.builder()
                .imageUrl("https://example.com/image1.jpg")
                .sortOrder(1)
                .imageType(ImageType.MAIN)
                .build();

        createRequest = ProductCreateRequest.builder()
                .name("테스트 상품")
                .description("테스트 상품 설명")
                .categoryId(10L)
                .basePrice(new BigDecimal("50000"))
                .discountRate(10)
                .deliveryType(DeliveryType.ROCKET)
                .options(List.of(optionRequest))
                .images(List.of(imageRequest))
                .build();

        updateRequest = ProductUpdateRequest.builder()
                .name("수정된 상품명")
                .description("수정된 설명")
                .categoryId(10L)
                .basePrice(new BigDecimal("60000"))
                .discountRate(15)
                .deliveryType(DeliveryType.ROCKET)
                .build();
    }

    // ========== 상품 등록 테스트 ==========

    @Nested
    @DisplayName("상품 등록")
    class CreateProduct {

        @Test
        @DisplayName("[성공] 상품 등록 - 정상적으로 상품이 등록된다")
        void createProduct_success() {
            // given
            given(sellerService.getActiveSeller(100L))
                    .willReturn(Mono.just(testSeller));
            given(categoryRepository.findByIdAndIsActiveTrue(10L))
                    .willReturn(Mono.just(testCategory));
            given(productRepository.save(any(Product.class)))
                    .willReturn(Mono.just(testProduct));
            given(productOptionRepository.save(any(ProductOption.class)))
                    .willReturn(Mono.just(testOption));
            given(productImageRepository.save(any(ProductImage.class)))
                    .willReturn(Mono.just(testImage));
            given(categoryRepository.findById(10L))
                    .willReturn(Mono.just(testCategory));

            // when
            Mono<ProductDetailResponse> result = productService.createProduct(100L, createRequest);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.getName()).isEqualTo("테스트 상품");
                        assertThat(response.getBasePrice()).isEqualByComparingTo(new BigDecimal("50000"));
                    })
                    .verifyComplete();

            verify(sellerService, times(1)).getActiveSeller(100L);
            verify(productRepository, times(1)).save(any(Product.class));
        }

        @Test
        @DisplayName("[실패] 상품 등록 - 판매자가 아닌 경우")
        void createProduct_fail_notSeller() {
            // given
            given(sellerService.getActiveSeller(100L))
                    .willReturn(Mono.error(new BusinessException(ErrorCode.SELLER_NOT_FOUND)));

            // when
            Mono<ProductDetailResponse> result = productService.createProduct(100L, createRequest);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.SELLER_NOT_FOUND
                    )
                    .verify();

            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("[실패] 상품 등록 - 판매자 상태가 비활성인 경우")
        void createProduct_fail_inactiveSeller() {
            // given
            given(sellerService.getActiveSeller(100L))
                    .willReturn(Mono.error(new BusinessException(ErrorCode.SELLER_NOT_ACTIVE)));

            // when
            Mono<ProductDetailResponse> result = productService.createProduct(100L, createRequest);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.SELLER_NOT_ACTIVE
                    )
                    .verify();
        }

        @Test
        @DisplayName("[실패] 상품 등록 - 카테고리가 존재하지 않는 경우")
        void createProduct_fail_categoryNotFound() {
            // given
            given(sellerService.getActiveSeller(100L))
                    .willReturn(Mono.just(testSeller));
            given(categoryRepository.findByIdAndIsActiveTrue(10L))
                    .willReturn(Mono.empty());

            // when
            Mono<ProductDetailResponse> result = productService.createProduct(100L, createRequest);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.CATEGORY_NOT_FOUND
                    )
                    .verify();

            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("[실패] 상품 등록 - DB 저장 실패")
        void createProduct_fail_databaseError() {
            // given
            given(sellerService.getActiveSeller(100L))
                    .willReturn(Mono.just(testSeller));
            given(categoryRepository.findByIdAndIsActiveTrue(10L))
                    .willReturn(Mono.just(testCategory));
            given(productRepository.save(any(Product.class)))
                    .willReturn(Mono.error(new RuntimeException("Database error")));

            // when
            Mono<ProductDetailResponse> result = productService.createProduct(100L, createRequest);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.INTERNAL_SERVER_ERROR
                    )
                    .verify();
        }

        @Test
        @DisplayName("[실패] 상품 등록 - 옵션 저장 실패")
        void createProduct_fail_optionSaveError() {
            // given
            given(sellerService.getActiveSeller(100L))
                    .willReturn(Mono.just(testSeller));
            given(categoryRepository.findByIdAndIsActiveTrue(10L))
                    .willReturn(Mono.just(testCategory));
            given(productRepository.save(any(Product.class)))
                    .willReturn(Mono.just(testProduct));
            given(productOptionRepository.save(any(ProductOption.class)))
                    .willReturn(Mono.error(new RuntimeException("Option save error")));
            given(productImageRepository.save(any(ProductImage.class)))
                    .willReturn(Mono.just(testImage));

            // when
            Mono<ProductDetailResponse> result = productService.createProduct(100L, createRequest);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.INTERNAL_SERVER_ERROR
                    )
                    .verify();
        }
    }

    // ========== 상품 상세 조회 테스트 ==========

    @Nested
    @DisplayName("상품 상세 조회")
    class GetProductDetail {

        @Test
        @DisplayName("[성공] 상품 상세 조회 - 정상적으로 상품 정보를 조회한다")
        void getProductDetail_success() {
            // given
            given(productRepository.findByIdAndStatusNot(1L, ProductStatus.DELETED))
                    .willReturn(Mono.just(testProduct));
            given(productRepository.save(any(Product.class)))
                    .willReturn(Mono.just(testProduct));
            given(productOptionRepository.findByProductId(1L))
                    .willReturn(Flux.just(testOption));
            given(productImageRepository.findByProductIdOrderBySortOrder(1L))
                    .willReturn(Flux.just(testImage));
            given(categoryRepository.findById(10L))
                    .willReturn(Mono.just(testCategory));

            // when
            Mono<ProductDetailResponse> result = productService.getProductDetail(1L);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.getName()).isEqualTo("테스트 상품");
                        assertThat(response.getBasePrice()).isEqualByComparingTo(new BigDecimal("50000"));
                        assertThat(response.getDiscountRate()).isEqualTo(10);
                    })
                    .verifyComplete();

            verify(productRepository, times(1)).findByIdAndStatusNot(1L, ProductStatus.DELETED);
        }

        @Test
        @DisplayName("[실패] 상품 상세 조회 - 존재하지 않는 상품")
        void getProductDetail_fail_notFound() {
            // given
            given(productRepository.findByIdAndStatusNot(999L, ProductStatus.DELETED))
                    .willReturn(Mono.empty());

            // when
            Mono<ProductDetailResponse> result = productService.getProductDetail(999L);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.PRODUCT_NOT_FOUND
                    )
                    .verify();
        }

        @Test
        @DisplayName("[실패] 상품 상세 조회 - 삭제된 상품")
        void getProductDetail_fail_deletedProduct() {
            // given
            given(productRepository.findByIdAndStatusNot(1L, ProductStatus.DELETED))
                    .willReturn(Mono.empty()); // DELETED 상태는 조회되지 않음

            // when
            Mono<ProductDetailResponse> result = productService.getProductDetail(1L);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.PRODUCT_NOT_FOUND
                    )
                    .verify();
        }

        @Test
        @DisplayName("[실패] 상품 상세 조회 - 카테고리 조회 실패")
        void getProductDetail_fail_categoryNotFound() {
            // given
            given(productRepository.findByIdAndStatusNot(1L, ProductStatus.DELETED))
                    .willReturn(Mono.just(testProduct));
            given(productRepository.save(any(Product.class)))
                    .willReturn(Mono.just(testProduct));
            given(productOptionRepository.findByProductId(1L))
                    .willReturn(Flux.just(testOption));
            given(productImageRepository.findByProductIdOrderBySortOrder(1L))
                    .willReturn(Flux.just(testImage));
            given(categoryRepository.findById(10L))
                    .willReturn(Mono.empty());

            // when
            Mono<ProductDetailResponse> result = productService.getProductDetail(1L);

            // then
            StepVerifier.create(result)
                    .expectError()
                    .verify();
        }

        @Test
        @DisplayName("[실패] 상품 상세 조회 - DB 조회 오류")
        void getProductDetail_fail_databaseError() {
            // given
            given(productRepository.findByIdAndStatusNot(1L, ProductStatus.DELETED))
                    .willReturn(Mono.error(new RuntimeException("Database error")));

            // when
            Mono<ProductDetailResponse> result = productService.getProductDetail(1L);

            // then
            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();
        }

        @Test
        @DisplayName("[실패] 상품 상세 조회 - 조회수 업데이트 실패")
        void getProductDetail_fail_viewCountUpdateError() {
            // given
            given(productRepository.findByIdAndStatusNot(1L, ProductStatus.DELETED))
                    .willReturn(Mono.just(testProduct));
            given(productRepository.save(any(Product.class)))
                    .willReturn(Mono.error(new RuntimeException("Update error")));

            // when
            Mono<ProductDetailResponse> result = productService.getProductDetail(1L);

            // then
            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }

    // ========== 상품 목록 검색 테스트 ==========

    @Nested
    @DisplayName("상품 목록 검색")
    class SearchProducts {

        @Test
        @DisplayName("[성공] 상품 목록 검색 - 카테고리별 인기순 조회")
        void searchProducts_success_byCategoryPopular() {
            // given
            ProductSearchRequest request = ProductSearchRequest.builder()
                    .categoryId(10L)
                    .sortType(ProductSearchRequest.SortType.POPULAR)
                    .page(1)
                    .size(20)
                    .build();

            given(productRepository.findByCategoryIdOrderBySalesCount(eq(10L), eq(20), eq(0)))
                    .willReturn(Flux.just(testProduct));
            given(productRepository.countByCategoryIdAndStatus(10L, ProductStatus.ON_SALE))
                    .willReturn(Mono.just(1L));
            given(productImageRepository.findFirstByProductId(1L))
                    .willReturn(Mono.just(testImage));

            // when
            Mono<PageResponse> result = productService.searchProducts(request);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.getContent()).hasSize(1);
                        assertThat(response.getTotalElements()).isEqualTo(1L);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("[성공] 상품 목록 검색 - 키워드 검색")
        void searchProducts_success_byKeyword() {
            // given
            ProductSearchRequest request = ProductSearchRequest.builder()
                    .keyword("테스트")
                    .page(1)
                    .size(20)
                    .build();

            given(productRepository.searchByKeyword(eq("테스트"), eq(20), eq(0)))
                    .willReturn(Flux.just(testProduct));
            given(productRepository.countByKeyword("테스트"))
                    .willReturn(Mono.just(1L));
            given(productImageRepository.findFirstByProductId(1L))
                    .willReturn(Mono.just(testImage));

            // when
            Mono<PageResponse> result = productService.searchProducts(request);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.getTotalElements()).isEqualTo(1L);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("[성공] 상품 목록 검색 - 카테고리 없이 조회시 빈 목록")
        void searchProducts_success_emptyWithoutCategory() {
            // given
            ProductSearchRequest request = ProductSearchRequest.builder()
                    .page(1)
                    .size(20)
                    .build();

            // when
            Mono<PageResponse> result = productService.searchProducts(request);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.getContent()).isEmpty();
                        assertThat(response.getTotalElements()).isEqualTo(0L);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("[성공] 상품 목록 검색 - 가격 낮은순 정렬")
        void searchProducts_success_priceAsc() {
            // given
            ProductSearchRequest request = ProductSearchRequest.builder()
                    .categoryId(10L)
                    .sortType(ProductSearchRequest.SortType.PRICE_ASC)
                    .page(1)
                    .size(20)
                    .build();

            given(productRepository.findByCategoryIdOrderByPriceAsc(eq(10L), eq(20), eq(0)))
                    .willReturn(Flux.just(testProduct));
            given(productRepository.countByCategoryIdAndStatus(10L, ProductStatus.ON_SALE))
                    .willReturn(Mono.just(1L));
            given(productImageRepository.findFirstByProductId(1L))
                    .willReturn(Mono.just(testImage));

            // when
            Mono<PageResponse> result = productService.searchProducts(request);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("[실패] 상품 목록 검색 - DB 조회 오류")
        void searchProducts_fail_databaseError() {
            // given
            ProductSearchRequest request = ProductSearchRequest.builder()
                    .categoryId(10L)
                    .sortType(ProductSearchRequest.SortType.POPULAR)
                    .page(1)
                    .size(20)
                    .build();

            given(productRepository.findByCategoryIdOrderBySalesCount(eq(10L), eq(20), eq(0)))
                    .willReturn(Flux.error(new RuntimeException("Database error")));
            given(productRepository.countByCategoryIdAndStatus(10L, ProductStatus.ON_SALE))
                    .willReturn(Mono.just(0L));

            // when
            Mono<PageResponse> result = productService.searchProducts(request);

            // then
            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }

    // ========== 상품 수정 테스트 ==========

    @Nested
    @DisplayName("상품 수정")
    class UpdateProduct {

        @Test
        @DisplayName("[성공] 상품 수정 - 정상적으로 상품 정보가 수정된다")
        void updateProduct_success() {
            // given
            given(sellerService.getActiveSeller(100L))
                    .willReturn(Mono.just(testSeller));
            given(productRepository.findByIdAndStatusNot(1L, ProductStatus.DELETED))
                    .willReturn(Mono.just(testProduct));
            given(productRepository.save(any(Product.class)))
                    .willReturn(Mono.just(testProduct));
            given(productOptionRepository.findByProductId(1L))
                    .willReturn(Flux.just(testOption));
            given(productImageRepository.findByProductIdOrderBySortOrder(1L))
                    .willReturn(Flux.just(testImage));
            given(categoryRepository.findById(10L))
                    .willReturn(Mono.just(testCategory));

            // when
            Mono<ProductDetailResponse> result = productService.updateProduct(100L, 1L, updateRequest);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                    })
                    .verifyComplete();

            verify(productRepository, times(1)).save(any(Product.class));
        }

        @Test
        @DisplayName("[실패] 상품 수정 - 존재하지 않는 상품")
        void updateProduct_fail_notFound() {
            // given
            given(sellerService.getActiveSeller(100L))
                    .willReturn(Mono.just(testSeller));
            given(productRepository.findByIdAndStatusNot(999L, ProductStatus.DELETED))
                    .willReturn(Mono.empty());

            // when
            Mono<ProductDetailResponse> result = productService.updateProduct(100L, 999L, updateRequest);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.PRODUCT_NOT_FOUND
                    )
                    .verify();

            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("[실패] 상품 수정 - 권한 없음 (다른 판매자의 상품)")
        void updateProduct_fail_accessDenied() {
            // given
            Seller otherSeller = Seller.builder()
                    .id(2L)
                    .memberId(200L)
                    .businessName("다른 판매자")
                    .businessNumber("999-99-99999")
                    .status(SellerStatus.ACTIVE)
                    .build();

            given(sellerService.getActiveSeller(200L))
                    .willReturn(Mono.just(otherSeller));
            given(productRepository.findByIdAndStatusNot(1L, ProductStatus.DELETED))
                    .willReturn(Mono.just(testProduct)); // sellerId가 1인 상품

            // when
            Mono<ProductDetailResponse> result = productService.updateProduct(200L, 1L, updateRequest);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.ACCESS_DENIED
                    )
                    .verify();

            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("[실패] 상품 수정 - 판매자가 아닌 경우")
        void updateProduct_fail_notSeller() {
            // given
            given(sellerService.getActiveSeller(100L))
                    .willReturn(Mono.error(new BusinessException(ErrorCode.SELLER_NOT_FOUND)));

            // when
            Mono<ProductDetailResponse> result = productService.updateProduct(100L, 1L, updateRequest);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.SELLER_NOT_FOUND
                    )
                    .verify();

            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("[실패] 상품 수정 - 카테고리 변경 시 존재하지 않는 카테고리")
        void updateProduct_fail_categoryNotFound() {
            // given
            ProductUpdateRequest requestWithNewCategory = ProductUpdateRequest.builder()
                    .name("수정된 상품명")
                    .categoryId(999L) // 존재하지 않는 카테고리
                    .build();

            given(sellerService.getActiveSeller(100L))
                    .willReturn(Mono.just(testSeller));
            given(productRepository.findByIdAndStatusNot(1L, ProductStatus.DELETED))
                    .willReturn(Mono.just(testProduct));
            given(categoryRepository.findByIdAndIsActiveTrue(999L))
                    .willReturn(Mono.empty());

            // when
            Mono<ProductDetailResponse> result = productService.updateProduct(100L, 1L, requestWithNewCategory);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.CATEGORY_NOT_FOUND
                    )
                    .verify();
        }

        @Test
        @DisplayName("[실패] 상품 수정 - DB 저장 실패")
        void updateProduct_fail_saveError() {
            // given
            given(sellerService.getActiveSeller(100L))
                    .willReturn(Mono.just(testSeller));
            given(productRepository.findByIdAndStatusNot(1L, ProductStatus.DELETED))
                    .willReturn(Mono.just(testProduct));
            given(productRepository.save(any(Product.class)))
                    .willReturn(Mono.error(new RuntimeException("Save error")));

            // when
            Mono<ProductDetailResponse> result = productService.updateProduct(100L, 1L, updateRequest);

            // then
            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }

    // ========== 상품 상태 변경 테스트 ==========

    @Nested
    @DisplayName("상품 상태 변경")
    class UpdateProductStatus {

        @Test
        @DisplayName("[성공] 상품 상태 변경 - 판매중지로 변경")
        void updateProductStatus_success_stopSale() {
            // given
            ProductStatusUpdateRequest statusRequest = ProductStatusUpdateRequest.builder()
                    .status(ProductStatus.STOP_SALE)
                    .build();

            given(sellerService.getActiveSeller(100L))
                    .willReturn(Mono.just(testSeller));
            given(productRepository.findByIdAndStatusNot(1L, ProductStatus.DELETED))
                    .willReturn(Mono.just(testProduct));
            given(productRepository.save(any(Product.class)))
                    .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            // when
            Mono<ProductResponse> result = productService.updateProductStatus(100L, 1L, statusRequest);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                    })
                    .verifyComplete();

            verify(productRepository, times(1)).save(any(Product.class));
        }

        @Test
        @DisplayName("[성공] 상품 상태 변경 - 품절로 변경")
        void updateProductStatus_success_soldOut() {
            // given
            ProductStatusUpdateRequest statusRequest = ProductStatusUpdateRequest.builder()
                    .status(ProductStatus.SOLD_OUT)
                    .build();

            given(sellerService.getActiveSeller(100L))
                    .willReturn(Mono.just(testSeller));
            given(productRepository.findByIdAndStatusNot(1L, ProductStatus.DELETED))
                    .willReturn(Mono.just(testProduct));
            given(productRepository.save(any(Product.class)))
                    .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            // when
            Mono<ProductResponse> result = productService.updateProductStatus(100L, 1L, statusRequest);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("[실패] 상품 상태 변경 - 존재하지 않는 상품")
        void updateProductStatus_fail_notFound() {
            // given
            ProductStatusUpdateRequest statusRequest = ProductStatusUpdateRequest.builder()
                    .status(ProductStatus.STOP_SALE)
                    .build();

            given(sellerService.getActiveSeller(100L))
                    .willReturn(Mono.just(testSeller));
            given(productRepository.findByIdAndStatusNot(999L, ProductStatus.DELETED))
                    .willReturn(Mono.empty());

            // when
            Mono<ProductResponse> result = productService.updateProductStatus(100L, 999L, statusRequest);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.PRODUCT_NOT_FOUND
                    )
                    .verify();
        }

        @Test
        @DisplayName("[실패] 상품 상태 변경 - 권한 없음")
        void updateProductStatus_fail_accessDenied() {
            // given
            ProductStatusUpdateRequest statusRequest = ProductStatusUpdateRequest.builder()
                    .status(ProductStatus.STOP_SALE)
                    .build();

            Seller otherSeller = Seller.builder()
                    .id(2L)
                    .memberId(200L)
                    .status(SellerStatus.ACTIVE)
                    .build();

            given(sellerService.getActiveSeller(200L))
                    .willReturn(Mono.just(otherSeller));
            given(productRepository.findByIdAndStatusNot(1L, ProductStatus.DELETED))
                    .willReturn(Mono.just(testProduct));

            // when
            Mono<ProductResponse> result = productService.updateProductStatus(200L, 1L, statusRequest);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.ACCESS_DENIED
                    )
                    .verify();
        }
    }

    // ========== 상품 삭제 테스트 ==========

    @Nested
    @DisplayName("상품 삭제")
    class DeleteProduct {

        @Test
        @DisplayName("[성공] 상품 삭제 - 정상적으로 소프트 삭제된다")
        void deleteProduct_success() {
            // given
            given(sellerService.getActiveSeller(100L))
                    .willReturn(Mono.just(testSeller));
            given(productRepository.findByIdAndStatusNot(1L, ProductStatus.DELETED))
                    .willReturn(Mono.just(testProduct));
            given(productRepository.save(any(Product.class)))
                    .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            // when
            Mono<Void> result = productService.deleteProduct(100L, 1L);

            // then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(productRepository, times(1)).save(any(Product.class));
        }

        @Test
        @DisplayName("[실패] 상품 삭제 - 존재하지 않는 상품")
        void deleteProduct_fail_notFound() {
            // given
            given(sellerService.getActiveSeller(100L))
                    .willReturn(Mono.just(testSeller));
            given(productRepository.findByIdAndStatusNot(999L, ProductStatus.DELETED))
                    .willReturn(Mono.empty());

            // when
            Mono<Void> result = productService.deleteProduct(100L, 999L);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.PRODUCT_NOT_FOUND
                    )
                    .verify();

            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("[실패] 상품 삭제 - 권한 없음 (다른 판매자의 상품)")
        void deleteProduct_fail_accessDenied() {
            // given
            Seller otherSeller = Seller.builder()
                    .id(2L)
                    .memberId(200L)
                    .status(SellerStatus.ACTIVE)
                    .build();

            given(sellerService.getActiveSeller(200L))
                    .willReturn(Mono.just(otherSeller));
            given(productRepository.findByIdAndStatusNot(1L, ProductStatus.DELETED))
                    .willReturn(Mono.just(testProduct));

            // when
            Mono<Void> result = productService.deleteProduct(200L, 1L);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.ACCESS_DENIED
                    )
                    .verify();

            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("[실패] 상품 삭제 - 이미 삭제된 상품")
        void deleteProduct_fail_alreadyDeleted() {
            // given
            Product deletedProduct = Product.builder()
                    .id(1L)
                    .sellerId(1L)
                    .name("삭제된 상품")
                    .status(ProductStatus.DELETED)
                    .build();

            given(sellerService.getActiveSeller(100L))
                    .willReturn(Mono.just(testSeller));
            given(productRepository.findByIdAndStatusNot(1L, ProductStatus.DELETED))
                    .willReturn(Mono.just(deletedProduct));

            // when
            Mono<Void> result = productService.deleteProduct(100L, 1L);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.PRODUCT_ALREADY_DELETED
                    )
                    .verify();
        }

        @Test
        @DisplayName("[실패] 상품 삭제 - 판매자가 아닌 경우")
        void deleteProduct_fail_notSeller() {
            // given
            given(sellerService.getActiveSeller(100L))
                    .willReturn(Mono.error(new BusinessException(ErrorCode.SELLER_NOT_FOUND)));

            // when
            Mono<Void> result = productService.deleteProduct(100L, 1L);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getErrorCode() == ErrorCode.SELLER_NOT_FOUND
                    )
                    .verify();

            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("[실패] 상품 삭제 - DB 저장 실패")
        void deleteProduct_fail_saveError() {
            // given
            given(sellerService.getActiveSeller(100L))
                    .willReturn(Mono.just(testSeller));
            given(productRepository.findByIdAndStatusNot(1L, ProductStatus.DELETED))
                    .willReturn(Mono.just(testProduct));
            given(productRepository.save(any(Product.class)))
                    .willReturn(Mono.error(new RuntimeException("Save error")));

            // when
            Mono<Void> result = productService.deleteProduct(100L, 1L);

            // then
            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }
}
