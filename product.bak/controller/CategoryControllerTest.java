package com.example.ecommerce.domain.product.controller;

import com.example.ecommerce.domain.product.dto.response.CategoryResponse;
import com.example.ecommerce.domain.product.dto.response.CategoryTreeResponse;
import com.example.ecommerce.domain.product.service.CategoryService;
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
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@WebFluxTest(CategoryController.class)
@AutoConfigureRestDocs
@Import({RestDocsConfiguration.class, SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("CategoryController 테스트")
class CategoryControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CategoryService categoryService;

    private CategoryResponse electronicsCategory;
    private CategoryResponse mobileCategory;
    private CategoryTreeResponse electronicsTreeResponse;

    @BeforeEach
    void setUp() {
        electronicsCategory = CategoryResponse.builder()
                .id(1L)
                .name("전자기기")
                .depth(0)
                .sortOrder(1)
                .parentId(null)
                .build();

        mobileCategory = CategoryResponse.builder()
                .id(2L)
                .name("휴대폰")
                .depth(1)
                .sortOrder(1)
                .parentId(1L)
                .build();

        CategoryTreeResponse mobileTreeResponse = CategoryTreeResponse.builder()
                .id(2L)
                .name("휴대폰")
                .depth(1)
                .sortOrder(1)
                .children(null)
                .build();

        electronicsTreeResponse = CategoryTreeResponse.builder()
                .id(1L)
                .name("전자기기")
                .depth(0)
                .sortOrder(1)
                .children(List.of(mobileTreeResponse))
                .build();
    }

    // ========== 카테고리 트리 조회 테스트 ==========

    @Nested
    @DisplayName("GET /api/v1/categories - 카테고리 트리 조회")
    class GetCategoryTree {

        @Test
        @DisplayName("[성공] 전체 카테고리 트리를 조회한다")
        void getCategoryTree_success() {
            // given
            given(categoryService.getCategoryTree())
                    .willReturn(Flux.just(electronicsTreeResponse));

            // when & then
            webTestClient.get()
                    .uri("/api/v1/categories")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data[0].id").isEqualTo(1)
                    .jsonPath("$.data[0].name").isEqualTo("전자기기")
                    .jsonPath("$.data[0].children").isArray()
                    .jsonPath("$.data[0].children[0].name").isEqualTo("휴대폰")
                    .consumeWith(document("category/get-tree-success",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            responseFields(
                                    fieldWithPath("success").description("요청 성공 여부"),
                                    fieldWithPath("message").description("응답 메시지"),
                                    fieldWithPath("data").description("카테고리 트리 목록"),
                                    fieldWithPath("data[].id").description("카테고리 ID"),
                                    fieldWithPath("data[].name").description("카테고리 이름"),
                                    fieldWithPath("data[].depth").description("카테고리 깊이 (0: 대분류, 1: 중분류, 2: 소분류)"),
                                    fieldWithPath("data[].sortOrder").description("정렬 순서"),
                                    fieldWithPath("data[].children").description("하위 카테고리 목록").optional(),
                                    fieldWithPath("data[].children[].id").description("하위 카테고리 ID").optional(),
                                    fieldWithPath("data[].children[].name").description("하위 카테고리 이름").optional(),
                                    fieldWithPath("data[].children[].depth").description("하위 카테고리 깊이").optional(),
                                    fieldWithPath("data[].children[].sortOrder").description("하위 카테고리 정렬 순서").optional(),
                                    fieldWithPath("data[].children[].children").description("하위의 하위 카테고리 목록").optional()
                            )
                    ));
        }

        @Test
        @DisplayName("[성공] 카테고리가 없을 경우 빈 목록을 반환한다")
        void getCategoryTree_success_empty() {
            // given
            given(categoryService.getCategoryTree())
                    .willReturn(Flux.empty());

            // when & then
            webTestClient.get()
                    .uri("/api/v1/categories")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data").isEmpty();
        }

        @Test
        @DisplayName("[실패] 서비스 오류 발생 시 500 에러를 반환한다")
        void getCategoryTree_fail_serviceError() {
            // given
            given(categoryService.getCategoryTree())
                    .willReturn(Flux.error(new RuntimeException("Database error")));

            // when & then
            webTestClient.get()
                    .uri("/api/v1/categories")
                    .exchange()
                    .expectStatus().is5xxServerError();
        }
    }

    // ========== 카테고리 상세 조회 테스트 ==========

    @Nested
    @DisplayName("GET /api/v1/categories/{id} - 카테고리 상세 조회")
    class GetCategory {

        @Test
        @DisplayName("[성공] 카테고리 상세 정보를 조회한다")
        void getCategory_success() {
            // given
            given(categoryService.getCategory(1L))
                    .willReturn(Mono.just(electronicsCategory));

            // when & then
            webTestClient.get()
                    .uri("/api/v1/categories/{id}", 1)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data.id").isEqualTo(1)
                    .jsonPath("$.data.name").isEqualTo("전자기기")
                    .jsonPath("$.data.depth").isEqualTo(0)
                    .consumeWith(document("category/get-detail-success",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            pathParameters(
                                    parameterWithName("id").description("카테고리 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").description("요청 성공 여부"),
                                    fieldWithPath("message").description("응답 메시지").optional(),
                                    fieldWithPath("data").description("카테고리 정보"),
                                    fieldWithPath("data.id").description("카테고리 ID"),
                                    fieldWithPath("data.name").description("카테고리 이름"),
                                    fieldWithPath("data.depth").description("카테고리 깊이"),
                                    fieldWithPath("data.sortOrder").description("정렬 순서"),
                                    fieldWithPath("data.parentId").description("부모 카테고리 ID").optional()
                            )
                    ));
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 카테고리 ID로 조회 시 404 에러를 반환한다")
        void getCategory_fail_notFound() {
            // given
            given(categoryService.getCategory(999L))
                    .willReturn(Mono.error(new BusinessException(ErrorCode.CATEGORY_NOT_FOUND)));

            // when & then
            webTestClient.get()
                    .uri("/api/v1/categories/{id}", 999)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.error.code").isEqualTo("P003");
        }

        @Test
        @DisplayName("[실패] 잘못된 카테고리 ID 형식으로 조회 시 400 에러를 반환한다")
        void getCategory_fail_invalidIdFormat() {
            // when & then
            webTestClient.get()
                    .uri("/api/v1/categories/invalid")
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("[실패] 음수 카테고리 ID로 조회 시 적절한 응답을 반환한다")
        void getCategory_fail_negativeId() {
            // given
            given(categoryService.getCategory(-1L))
                    .willReturn(Mono.error(new BusinessException(ErrorCode.CATEGORY_NOT_FOUND)));

            // when & then
            webTestClient.get()
                    .uri("/api/v1/categories/{id}", -1)
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        @DisplayName("[실패] 서비스 오류 발생 시 500 에러를 반환한다")
        void getCategory_fail_serviceError() {
            // given
            given(categoryService.getCategory(1L))
                    .willReturn(Mono.error(new RuntimeException("Database error")));

            // when & then
            webTestClient.get()
                    .uri("/api/v1/categories/{id}", 1)
                    .exchange()
                    .expectStatus().is5xxServerError();
        }
    }

    // ========== 하위 카테고리 조회 테스트 ==========

    @Nested
    @DisplayName("GET /api/v1/categories/{id}/children - 하위 카테고리 조회")
    class GetChildCategories {

        @Test
        @DisplayName("[성공] 하위 카테고리 목록을 조회한다")
        void getChildCategories_success() {
            // given
            given(categoryService.getChildCategories(1L))
                    .willReturn(Flux.just(mobileCategory));

            // when & then
            webTestClient.get()
                    .uri("/api/v1/categories/{id}/children", 1)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data[0].id").isEqualTo(2)
                    .jsonPath("$.data[0].name").isEqualTo("휴대폰")
                    .jsonPath("$.data[0].parentId").isEqualTo(1)
                    .consumeWith(document("category/get-children-success",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            pathParameters(
                                    parameterWithName("id").description("부모 카테고리 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").description("요청 성공 여부"),
                                    fieldWithPath("message").description("응답 메시지"),
                                    fieldWithPath("data").description("하위 카테고리 목록"),
                                    fieldWithPath("data[].id").description("카테고리 ID"),
                                    fieldWithPath("data[].name").description("카테고리 이름"),
                                    fieldWithPath("data[].depth").description("카테고리 깊이"),
                                    fieldWithPath("data[].sortOrder").description("정렬 순서"),
                                    fieldWithPath("data[].parentId").description("부모 카테고리 ID")
                            )
                    ));
        }

        @Test
        @DisplayName("[성공] 하위 카테고리가 없을 경우 빈 목록을 반환한다")
        void getChildCategories_success_empty() {
            // given
            given(categoryService.getChildCategories(99L))
                    .willReturn(Flux.empty());

            // when & then
            webTestClient.get()
                    .uri("/api/v1/categories/{id}/children", 99)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data").isEmpty();
        }

        @Test
        @DisplayName("[성공] 여러 하위 카테고리를 조회한다")
        void getChildCategories_success_multiple() {
            // given
            CategoryResponse laptopCategory = CategoryResponse.builder()
                    .id(3L)
                    .name("노트북")
                    .depth(1)
                    .sortOrder(2)
                    .parentId(1L)
                    .build();

            given(categoryService.getChildCategories(1L))
                    .willReturn(Flux.just(mobileCategory, laptopCategory));

            // when & then
            webTestClient.get()
                    .uri("/api/v1/categories/{id}/children", 1)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data.length()").isEqualTo(2);
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 부모 카테고리 ID로 조회 시 빈 목록을 반환한다")
        void getChildCategories_emptyForNonExistentParent() {
            // given
            given(categoryService.getChildCategories(999L))
                    .willReturn(Flux.empty());

            // when & then
            webTestClient.get()
                    .uri("/api/v1/categories/{id}/children", 999)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.data").isEmpty();
        }

        @Test
        @DisplayName("[실패] 잘못된 ID 형식으로 조회 시 400 에러를 반환한다")
        void getChildCategories_fail_invalidIdFormat() {
            // when & then
            webTestClient.get()
                    .uri("/api/v1/categories/invalid/children")
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("[실패] 서비스 오류 발생 시 500 에러를 반환한다")
        void getChildCategories_fail_serviceError() {
            // given
            given(categoryService.getChildCategories(1L))
                    .willReturn(Flux.error(new RuntimeException("Database error")));

            // when & then
            webTestClient.get()
                    .uri("/api/v1/categories/{id}/children", 1)
                    .exchange()
                    .expectStatus().is5xxServerError();
        }
    }
}
