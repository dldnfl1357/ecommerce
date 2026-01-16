package com.example.ecommerce.product.domain.product.service;

import com.example.ecommerce.common.exception.BusinessException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.product.domain.inventory.entity.Inventory;
import com.example.ecommerce.product.domain.inventory.repository.InventoryRepository;
import com.example.ecommerce.product.domain.product.dto.request.ProductCreateRequest;
import com.example.ecommerce.product.domain.product.dto.response.ProductImageResponse;
import com.example.ecommerce.product.domain.product.dto.response.ProductOptionResponse;
import com.example.ecommerce.product.domain.product.dto.response.ProductResponse;
import com.example.ecommerce.product.domain.product.entity.Product;
import com.example.ecommerce.product.domain.product.entity.ProductImage;
import com.example.ecommerce.product.domain.product.entity.ProductOption;
import com.example.ecommerce.product.domain.product.entity.ProductStatus;
import com.example.ecommerce.product.domain.product.repository.ProductImageRepository;
import com.example.ecommerce.product.domain.product.repository.ProductOptionRepository;
import com.example.ecommerce.product.domain.product.repository.ProductRepository;
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
    private final InventoryRepository inventoryRepository;

    @Transactional
    public Mono<ProductResponse> createProduct(Long sellerId, ProductCreateRequest request) {
        Product product = Product.builder()
                .sellerId(sellerId)
                .categoryId(request.getCategoryId())
                .name(request.getName())
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .discountRate(request.getDiscountRate())
                .isRocketDelivery(request.getIsRocketDelivery())
                .isRocketWow(request.getIsRocketWow())
                .build();

        return productRepository.save(product)
                .flatMap(savedProduct -> {
                    Mono<List<ProductOption>> optionsMono = Flux.fromIterable(request.getOptions())
                            .flatMap(optionReq -> {
                                ProductOption option = ProductOption.builder()
                                        .productId(savedProduct.getId())
                                        .name(optionReq.getName())
                                        .additionalPrice(optionReq.getAdditionalPrice())
                                        .build();
                                return productOptionRepository.save(option)
                                        .flatMap(savedOption -> {
                                            Inventory inventory = Inventory.builder()
                                                    .productOptionId(savedOption.getId())
                                                    .quantity(optionReq.getInitialStock() != null ? optionReq.getInitialStock() : 0)
                                                    .build();
                                            return inventoryRepository.save(inventory)
                                                    .thenReturn(savedOption);
                                        });
                            })
                            .collectList();

                    Mono<List<ProductImage>> imagesMono = request.getImages() != null
                            ? Flux.fromIterable(request.getImages())
                            .map(imageReq -> ProductImage.builder()
                                    .productId(savedProduct.getId())
                                    .imageUrl(imageReq.getImageUrl())
                                    .displayOrder(imageReq.getDisplayOrder())
                                    .isMain(imageReq.getIsMain())
                                    .build())
                            .flatMap(productImageRepository::save)
                            .collectList()
                            : Mono.just(List.of());

                    return Mono.zip(optionsMono, imagesMono)
                            .map(tuple -> ProductResponse.from(
                                    savedProduct,
                                    tuple.getT1().stream().map(ProductOptionResponse::from).toList(),
                                    tuple.getT2().stream().map(ProductImageResponse::from).toList()
                            ));
                })
                .doOnSuccess(response -> log.info("상품 생성 완료: productId={}", response.getId()));
    }

    public Mono<ProductResponse> getProduct(Long productId) {
        return productRepository.findById(productId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)))
                .flatMap(this::enrichProductResponse);
    }

    public Flux<ProductResponse> getProductsByCategory(Long categoryId, int page, int size) {
        int offset = (page - 1) * size;
        return productRepository.findByCategoryWithPaging(categoryId, size, offset)
                .flatMap(this::enrichProductResponse);
    }

    public Flux<ProductResponse> searchProducts(String keyword) {
        return productRepository.searchByName(keyword)
                .flatMap(this::enrichProductResponse);
    }

    @Transactional
    public Mono<ProductResponse> publishProduct(Long productId, Long sellerId) {
        return productRepository.findById(productId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)))
                .flatMap(product -> {
                    if (!product.getSellerId().equals(sellerId)) {
                        return Mono.error(new BusinessException(ErrorCode.ACCESS_DENIED));
                    }
                    return productRepository.save(product.publish());
                })
                .flatMap(this::enrichProductResponse)
                .doOnSuccess(response -> log.info("상품 발행 완료: productId={}", productId));
    }

    @Transactional
    public Mono<Void> deleteProduct(Long productId, Long sellerId) {
        return productRepository.findById(productId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)))
                .flatMap(product -> {
                    if (!product.getSellerId().equals(sellerId)) {
                        return Mono.error(new BusinessException(ErrorCode.ACCESS_DENIED));
                    }
                    if (product.isDeleted()) {
                        return Mono.error(new BusinessException(ErrorCode.PRODUCT_ALREADY_DELETED));
                    }
                    return productRepository.save(product.delete());
                })
                .doOnSuccess(product -> log.info("상품 삭제 완료: productId={}", productId))
                .then();
    }

    private Mono<ProductResponse> enrichProductResponse(Product product) {
        Mono<List<ProductOptionResponse>> optionsMono = productOptionRepository
                .findByProductIdAndIsActiveTrue(product.getId())
                .map(ProductOptionResponse::from)
                .collectList();

        Mono<List<ProductImageResponse>> imagesMono = productImageRepository
                .findByProductIdOrderByDisplayOrderAsc(product.getId())
                .map(ProductImageResponse::from)
                .collectList();

        return Mono.zip(optionsMono, imagesMono)
                .map(tuple -> ProductResponse.from(product, tuple.getT1(), tuple.getT2()));
    }
}
