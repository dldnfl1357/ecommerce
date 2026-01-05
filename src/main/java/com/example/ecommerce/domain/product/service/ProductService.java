package com.example.ecommerce.domain.product.service;

import com.example.ecommerce.domain.product.dto.request.ProductCreateRequest;
import com.example.ecommerce.domain.product.dto.request.ProductSearchRequest;
import com.example.ecommerce.domain.product.dto.request.ProductStatusUpdateRequest;
import com.example.ecommerce.domain.product.dto.request.ProductUpdateRequest;
import com.example.ecommerce.domain.product.dto.response.ProductDetailResponse;
import com.example.ecommerce.domain.product.dto.response.ProductListResponse;
import com.example.ecommerce.domain.product.dto.response.ProductResponse;
import com.example.ecommerce.domain.product.entity.Category;
import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.product.entity.ProductImage;
import com.example.ecommerce.domain.product.entity.ProductOption;
import com.example.ecommerce.domain.product.entity.ProductStatus;
import com.example.ecommerce.domain.product.repository.CategoryRepository;
import com.example.ecommerce.domain.product.repository.ProductImageRepository;
import com.example.ecommerce.domain.product.repository.ProductOptionRepository;
import com.example.ecommerce.domain.product.repository.ProductRepository;
import com.example.ecommerce.domain.seller.entity.Seller;
import com.example.ecommerce.domain.seller.service.SellerService;
import com.example.ecommerce.global.common.PageResponse;
import com.example.ecommerce.global.exception.BusinessException;
import com.example.ecommerce.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final SellerService sellerService;

    // ========== 상품 등록 ==========

    /**
     * 상품 등록 (판매자)
     */
    @Transactional
    public Mono<ProductDetailResponse> createProduct(Long memberId, ProductCreateRequest request) {
        return sellerService.getActiveSeller(memberId)
                .flatMap(seller -> validateCategoryAndCreate(seller.getId(), request))
                .flatMap(productRepository::save)
                .flatMap(product -> saveOptionsAndImages(product, request))
                .doOnSuccess(response -> log.info("상품 등록 완료: productId={}, memberId={}",
                        response.getId(), memberId))
                .onErrorMap(this::mapToBusinessException);
    }

    // ========== 상품 조회 ==========

    /**
     * 상품 상세 조회
     */
    public Mono<ProductDetailResponse> getProductDetail(Long productId) {
        return productRepository.findByIdAndStatusNot(productId, ProductStatus.DELETED)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)))
                .flatMap(this::increaseViewCountAndBuildDetailResponse)
                .doOnSuccess(response -> log.info("상품 상세 조회: productId={}", productId));
    }

    /**
     * 상품 목록 조회 (필터링/정렬/페이징)
     */
    public Mono<PageResponse<ProductListResponse>> searchProducts(ProductSearchRequest request) {
        Flux<Product> products = findProductsBySearchCriteria(request);
        Mono<Long> totalCount = countProductsBySearchCriteria(request);

        return products
                .flatMap(this::mapToProductListResponse)
                .collectList()
                .zipWith(totalCount)
                .map(tuple -> PageResponse.of(
                        tuple.getT1(),
                        tuple.getT2(),
                        request.getPage(),
                        request.getSize()
                ));
    }

    // ========== 상품 수정 ==========

    /**
     * 상품 수정 (판매자)
     */
    @Transactional
    public Mono<ProductDetailResponse> updateProduct(Long memberId, Long productId, ProductUpdateRequest request) {
        return sellerService.getActiveSeller(memberId)
                .flatMap(seller -> getProductAndValidateOwner(productId, seller.getId()))
                .flatMap(product -> updateProductInfo(product, request))
                .flatMap(productRepository::save)
                .flatMap(this::buildDetailResponse)
                .doOnSuccess(response -> log.info("상품 수정 완료: productId={}", productId));
    }

    /**
     * 상품 상태 변경 (판매자)
     */
    @Transactional
    public Mono<ProductResponse> updateProductStatus(Long memberId, Long productId, ProductStatusUpdateRequest request) {
        return sellerService.getActiveSeller(memberId)
                .flatMap(seller -> getProductAndValidateOwner(productId, seller.getId()))
                .map(product -> updateStatusByType(product, request.getStatus()))
                .flatMap(productRepository::save)
                .map(ProductResponse::from)
                .doOnSuccess(response -> log.info("상품 상태 변경: productId={}, status={}",
                        productId, request.getStatus()));
    }

    // ========== 상품 삭제 ==========

    /**
     * 상품 삭제 (소프트 삭제)
     */
    @Transactional
    public Mono<Void> deleteProduct(Long memberId, Long productId) {
        return sellerService.getActiveSeller(memberId)
                .flatMap(seller -> getProductAndValidateOwner(productId, seller.getId()))
                .flatMap(this::validateCanDelete)
                .map(Product::delete)
                .flatMap(productRepository::save)
                .doOnSuccess(product -> log.info("상품 삭제 완료: productId={}", productId))
                .then();
    }

    // ========== Private Helper Methods ==========

    private Mono<Product> validateCategoryAndCreate(Long sellerId, ProductCreateRequest request) {
        return categoryRepository.findByIdAndIsActiveTrue(request.getCategoryId())
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.CATEGORY_NOT_FOUND)))
                .map(category -> request.toEntity(sellerId));
    }

    private Mono<ProductDetailResponse> saveOptionsAndImages(Product product, ProductCreateRequest request) {
        Flux<ProductOption> optionFlux = Flux.fromIterable(request.getOptions())
                .map(optReq -> optReq.toEntity(product.getId()))
                .flatMap(productOptionRepository::save);

        // 첫 번째 이미지를 썸네일로 설정
        List<ProductImage> imageEntities = request.getImages().stream()
                .map(imgReq -> imgReq.toEntity(product.getId()))
                .toList();

        if (!imageEntities.isEmpty()) {
            imageEntities.get(0).setAsThumbnail();
        }

        Flux<ProductImage> imageFlux = Flux.fromIterable(imageEntities)
                .flatMap(productImageRepository::save);

        return Mono.zip(optionFlux.collectList(), imageFlux.collectList())
                .flatMap(tuple -> categoryRepository.findById(product.getCategoryId())
                        .map(category -> ProductDetailResponse.of(
                                product, category, tuple.getT1(), tuple.getT2()
                        ))
                );
    }

    private Mono<Product> getProductAndValidateOwner(Long productId, Long sellerId) {
        return productRepository.findByIdAndStatusNot(productId, ProductStatus.DELETED)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)))
                .flatMap(product -> {
                    if (!product.isOwnedBy(sellerId)) {
                        return Mono.error(new BusinessException(ErrorCode.ACCESS_DENIED));
                    }
                    return Mono.just(product);
                });
    }

    private Mono<Product> updateProductInfo(Product product, ProductUpdateRequest request) {
        // 카테고리 변경 시 검증
        if (request.getCategoryId() != null && !request.getCategoryId().equals(product.getCategoryId())) {
            return categoryRepository.findByIdAndIsActiveTrue(request.getCategoryId())
                    .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.CATEGORY_NOT_FOUND)))
                    .map(category -> {
                        product.updateInfo(request.getName(), request.getDescription(),
                                request.getCategoryId(), request.getDeliveryType());
                        if (request.getBasePrice() != null || request.getDiscountRate() != null) {
                            product.updatePrice(request.getBasePrice(), request.getDiscountRate());
                        }
                        return product;
                    });
        }

        product.updateInfo(request.getName(), request.getDescription(),
                request.getCategoryId(), request.getDeliveryType());
        if (request.getBasePrice() != null || request.getDiscountRate() != null) {
            product.updatePrice(request.getBasePrice(), request.getDiscountRate());
        }
        return Mono.just(product);
    }

    private Product updateStatusByType(Product product, ProductStatus newStatus) {
        return switch (newStatus) {
            case ON_SALE -> product.activate();
            case STOP_SALE -> product.deactivate();
            case SOLD_OUT -> product.markAsSoldOut();
            case DELETED -> product.delete();
        };
    }

    private Mono<Product> validateCanDelete(Product product) {
        // 추후 주문 진행 중 확인 로직 추가 가능
        if (product.isDeleted()) {
            return Mono.error(new BusinessException(ErrorCode.PRODUCT_ALREADY_DELETED));
        }
        return Mono.just(product);
    }

    private Flux<Product> findProductsBySearchCriteria(ProductSearchRequest request) {
        // 키워드 검색
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            return productRepository.searchByKeyword(
                    request.getKeyword(), request.getSize(), request.getOffset()
            );
        }

        // 카테고리 필수
        if (request.getCategoryId() == null) {
            return Flux.empty();
        }

        Long categoryId = request.getCategoryId();
        int limit = request.getSize();
        int offset = request.getOffset();

        return switch (request.getSortType()) {
            case POPULAR -> productRepository.findByCategoryIdOrderBySalesCount(categoryId, limit, offset);
            case NEWEST -> productRepository.findByCategoryIdOrderByNewest(categoryId, limit, offset);
            case PRICE_ASC -> productRepository.findByCategoryIdOrderByPriceAsc(categoryId, limit, offset);
            case PRICE_DESC -> productRepository.findByCategoryIdOrderByPriceDesc(categoryId, limit, offset);
            case RATING -> productRepository.findByCategoryIdOrderByRating(categoryId, limit, offset);
            case REVIEW -> productRepository.findByCategoryIdOrderByReviewCount(categoryId, limit, offset);
        };
    }

    private Mono<Long> countProductsBySearchCriteria(ProductSearchRequest request) {
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            return productRepository.countByKeyword(request.getKeyword());
        }
        if (request.getCategoryId() == null) {
            return Mono.just(0L);
        }
        return productRepository.countByCategoryIdAndStatus(
                request.getCategoryId(), ProductStatus.ON_SALE
        );
    }

    private Mono<ProductListResponse> mapToProductListResponse(Product product) {
        return productImageRepository.findFirstByProductId(product.getId())
                .map(ProductImage::getImageUrl)
                .defaultIfEmpty("")
                .map(thumbnailUrl -> ProductListResponse.from(product, thumbnailUrl));
    }

    private Mono<ProductDetailResponse> increaseViewCountAndBuildDetailResponse(Product product) {
        return productRepository.save(product.increaseViewCount())
                .flatMap(this::buildDetailResponse);
    }

    private Mono<ProductDetailResponse> buildDetailResponse(Product product) {
        Mono<List<ProductOption>> options = productOptionRepository
                .findByProductId(product.getId())
                .collectList();

        Mono<List<ProductImage>> images = productImageRepository
                .findByProductIdOrderBySortOrder(product.getId())
                .collectList();

        Mono<Category> category = categoryRepository.findById(product.getCategoryId());

        return Mono.zip(category, options, images)
                .map(tuple -> ProductDetailResponse.of(
                        product, tuple.getT1(), tuple.getT2(), tuple.getT3()
                ));
    }

    private Throwable mapToBusinessException(Throwable error) {
        if (error instanceof BusinessException) {
            return error;
        }
        log.error("예상치 못한 오류 발생", error);
        return new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
