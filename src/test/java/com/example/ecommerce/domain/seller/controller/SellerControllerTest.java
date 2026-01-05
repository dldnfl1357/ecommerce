package com.example.ecommerce.domain.seller.controller;

import com.example.ecommerce.domain.seller.dto.request.SellerRegisterRequest;
import com.example.ecommerce.domain.seller.dto.request.SellerUpdateRequest;
import com.example.ecommerce.domain.seller.dto.response.SellerResponse;
import com.example.ecommerce.domain.seller.entity.SellerStatus;
import com.example.ecommerce.domain.seller.service.SellerService;
import com.example.ecommerce.global.auth.JwtTokenProvider;
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
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@WebFluxTest(SellerController.class)
@AutoConfigureRestDocs
@Import({RestDocsConfiguration.class, SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("SellerController 테스트")
class SellerControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private SellerService sellerService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private SellerResponse sellerResponse;
    private SellerRegisterRequest registerRequest;
    private SellerUpdateRequest updateRequest;
    private String validToken;

    @BeforeEach
    void setUp() {
        validToken = "Bearer test-jwt-token";

        sellerResponse = SellerResponse.builder()
                .id(1L)
                .memberId(100L)
                .businessName("테스트 판매자")
                .businessNumber("123-45-67890")
                .representativeName("홍길동")
                .businessAddress("서울시 강남구")
                .contactNumber("02-1234-5678")
                .status(SellerStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
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

        // JWT 토큰 검증 설정
        given(jwtTokenProvider.validateToken(any()))
                .willReturn(true);
        given(jwtTokenProvider.getMemberId(any()))
                .willReturn(100L);
    }

    // ========== 판매자 등록 테스트 ==========

    @Nested
    @DisplayName("POST /api/v1/sellers - 판매자 등록")
    class RegisterSeller {

        @Test
        @DisplayName("[성공] 판매자 등록이 완료된다")
        void registerSeller_success() {
            // given
            given(sellerService.registerSeller(eq(100L), any(SellerRegisterRequest.class)))
                    .willReturn(Mono.just(sellerResponse));

            // when & then
            webTestClient.post()
                    .uri("/api/v1/sellers")
                    .header("Authorization", validToken)
                    .attribute("memberId", 100L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(registerRequest)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.message").isEqualTo("판매자 등록이 완료되었습니다.")
                    .jsonPath("$.data.businessName").isEqualTo("테스트 판매자")
                    .jsonPath("$.data.status").isEqualTo("ACTIVE")
                    .consumeWith(document("seller/register-success",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                    fieldWithPath("businessName").description("사업자명"),
                                    fieldWithPath("businessNumber").description("사업자 등록번호"),
                                    fieldWithPath("representativeName").description("대표자명"),
                                    fieldWithPath("businessAddress").description("사업장 주소"),
                                    fieldWithPath("contactNumber").description("연락처")
                            ),
                            responseFields(
                                    fieldWithPath("success").description("요청 성공 여부"),
                                    fieldWithPath("message").description("응답 메시지"),
                                    fieldWithPath("data").description("판매자 정보"),
                                    fieldWithPath("data.id").description("판매자 ID"),
                                    fieldWithPath("data.memberId").description("회원 ID"),
                                    fieldWithPath("data.businessName").description("사업자명"),
                                    fieldWithPath("data.businessNumber").description("사업자 등록번호 (마스킹)"),
                                    fieldWithPath("data.representativeName").description("대표자명"),
                                    fieldWithPath("data.businessAddress").description("사업장 주소"),
                                    fieldWithPath("data.contactNumber").description("연락처"),
                                    fieldWithPath("data.status").description("판매자 상태"),
                                    fieldWithPath("data.createdAt").description("등록일시")
                            )
                    ));
        }

        @Test
        @DisplayName("[실패] 이미 판매자인 경우 등록 실패")
        void registerSeller_fail_alreadySeller() {
            // given
            given(sellerService.registerSeller(eq(100L), any(SellerRegisterRequest.class)))
                    .willReturn(Mono.error(new BusinessException(ErrorCode.ALREADY_SELLER)));

            // when & then
            webTestClient.post()
                    .uri("/api/v1/sellers")
                    .header("Authorization", validToken)
                    .attribute("memberId", 100L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(registerRequest)
                    .exchange()
                    .expectStatus().isEqualTo(409) // CONFLICT
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.error.code").isEqualTo("S003");
        }

        @Test
        @DisplayName("[실패] 사업자 등록번호 중복")
        void registerSeller_fail_duplicateBusinessNumber() {
            // given
            given(sellerService.registerSeller(eq(100L), any(SellerRegisterRequest.class)))
                    .willReturn(Mono.error(new BusinessException(ErrorCode.DUPLICATE_BUSINESS_NUMBER)));

            // when & then
            webTestClient.post()
                    .uri("/api/v1/sellers")
                    .header("Authorization", validToken)
                    .attribute("memberId", 100L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(registerRequest)
                    .exchange()
                    .expectStatus().isEqualTo(409) // CONFLICT
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.error.code").isEqualTo("S002");
        }

        @Test
        @DisplayName("[실패] 사업자명 누락")
        void registerSeller_fail_missingBusinessName() {
            // given
            SellerRegisterRequest invalidRequest = SellerRegisterRequest.builder()
                    .businessNumber("123-45-67890")
                    .representativeName("홍길동")
                    .businessAddress("서울시 강남구")
                    .contactNumber("02-1234-5678")
                    .build();

            // when & then
            webTestClient.post()
                    .uri("/api/v1/sellers")
                    .header("Authorization", validToken)
                    .attribute("memberId", 100L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(invalidRequest)
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("[실패] 사업자 등록번호 누락")
        void registerSeller_fail_missingBusinessNumber() {
            // given
            SellerRegisterRequest invalidRequest = SellerRegisterRequest.builder()
                    .businessName("테스트 판매자")
                    .representativeName("홍길동")
                    .businessAddress("서울시 강남구")
                    .contactNumber("02-1234-5678")
                    .build();

            // when & then
            webTestClient.post()
                    .uri("/api/v1/sellers")
                    .header("Authorization", validToken)
                    .attribute("memberId", 100L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(invalidRequest)
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("[실패] 서버 오류 발생")
        void registerSeller_fail_serverError() {
            // given
            given(sellerService.registerSeller(eq(100L), any(SellerRegisterRequest.class)))
                    .willReturn(Mono.error(new RuntimeException("Internal error")));

            // when & then
            webTestClient.post()
                    .uri("/api/v1/sellers")
                    .header("Authorization", validToken)
                    .attribute("memberId", 100L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(registerRequest)
                    .exchange()
                    .expectStatus().is5xxServerError();
        }
    }

    // ========== 판매자 정보 조회 테스트 ==========

    @Nested
    @DisplayName("GET /api/v1/sellers/me - 내 판매자 정보 조회")
    class GetMySeller {

        @Test
        @DisplayName("[성공] 내 판매자 정보를 조회한다")
        void getMySeller_success() {
            // given
            given(sellerService.getMySeller(100L))
                    .willReturn(Mono.just(sellerResponse));

            // when & then
            webTestClient.get()
                    .uri("/api/v1/sellers/me")
                    .header("Authorization", validToken)
                    .attribute("memberId", 100L)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data.id").isEqualTo(1)
                    .jsonPath("$.data.businessName").isEqualTo("테스트 판매자")
                    .consumeWith(document("seller/get-me-success",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            responseFields(
                                    fieldWithPath("success").description("요청 성공 여부"),
                                    fieldWithPath("message").description("응답 메시지").optional(),
                                    fieldWithPath("data").description("판매자 정보"),
                                    fieldWithPath("data.id").description("판매자 ID"),
                                    fieldWithPath("data.memberId").description("회원 ID"),
                                    fieldWithPath("data.businessName").description("사업자명"),
                                    fieldWithPath("data.businessNumber").description("사업자 등록번호 (마스킹)"),
                                    fieldWithPath("data.representativeName").description("대표자명"),
                                    fieldWithPath("data.businessAddress").description("사업장 주소"),
                                    fieldWithPath("data.contactNumber").description("연락처"),
                                    fieldWithPath("data.status").description("판매자 상태"),
                                    fieldWithPath("data.createdAt").description("등록일시")
                            )
                    ));
        }

        @Test
        @DisplayName("[실패] 판매자가 아닌 경우")
        void getMySeller_fail_notSeller() {
            // given
            given(sellerService.getMySeller(100L))
                    .willReturn(Mono.error(new BusinessException(ErrorCode.SELLER_NOT_FOUND)));

            // when & then
            webTestClient.get()
                    .uri("/api/v1/sellers/me")
                    .header("Authorization", validToken)
                    .attribute("memberId", 100L)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.error.code").isEqualTo("S001");
        }

        @Test
        @DisplayName("[실패] 서버 오류 발생")
        void getMySeller_fail_serverError() {
            // given
            given(sellerService.getMySeller(100L))
                    .willReturn(Mono.error(new RuntimeException("Internal error")));

            // when & then
            webTestClient.get()
                    .uri("/api/v1/sellers/me")
                    .header("Authorization", validToken)
                    .attribute("memberId", 100L)
                    .exchange()
                    .expectStatus().is5xxServerError();
        }
    }

    // ========== 판매자 정보 수정 테스트 ==========

    @Nested
    @DisplayName("PUT /api/v1/sellers/me - 판매자 정보 수정")
    class UpdateSeller {

        @Test
        @DisplayName("[성공] 판매자 정보를 수정한다")
        void updateSeller_success() {
            // given
            SellerResponse updatedResponse = SellerResponse.builder()
                    .id(1L)
                    .memberId(100L)
                    .businessName("수정된 판매자")
                    .businessNumber("123-45-67890")
                    .representativeName("홍길동")
                    .businessAddress("서울시 서초구")
                    .contactNumber("02-9999-8888")
                    .status(SellerStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build();

            given(sellerService.updateSeller(eq(100L), any(SellerUpdateRequest.class)))
                    .willReturn(Mono.just(updatedResponse));

            // when & then
            webTestClient.put()
                    .uri("/api/v1/sellers/me")
                    .header("Authorization", validToken)
                    .attribute("memberId", 100L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(updateRequest)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.message").isEqualTo("판매자 정보가 수정되었습니다.")
                    .jsonPath("$.data.businessName").isEqualTo("수정된 판매자")
                    .jsonPath("$.data.businessAddress").isEqualTo("서울시 서초구")
                    .consumeWith(document("seller/update-success",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                    fieldWithPath("businessName").description("사업자명").optional(),
                                    fieldWithPath("businessAddress").description("사업장 주소").optional(),
                                    fieldWithPath("contactNumber").description("연락처").optional()
                            ),
                            responseFields(
                                    fieldWithPath("success").description("요청 성공 여부"),
                                    fieldWithPath("message").description("응답 메시지"),
                                    fieldWithPath("data").description("판매자 정보"),
                                    fieldWithPath("data.id").description("판매자 ID"),
                                    fieldWithPath("data.memberId").description("회원 ID"),
                                    fieldWithPath("data.businessName").description("사업자명"),
                                    fieldWithPath("data.businessNumber").description("사업자 등록번호 (마스킹)"),
                                    fieldWithPath("data.representativeName").description("대표자명"),
                                    fieldWithPath("data.businessAddress").description("사업장 주소"),
                                    fieldWithPath("data.contactNumber").description("연락처"),
                                    fieldWithPath("data.status").description("판매자 상태"),
                                    fieldWithPath("data.createdAt").description("등록일시")
                            )
                    ));
        }

        @Test
        @DisplayName("[실패] 판매자가 아닌 경우 수정 실패")
        void updateSeller_fail_notSeller() {
            // given
            given(sellerService.updateSeller(eq(100L), any(SellerUpdateRequest.class)))
                    .willReturn(Mono.error(new BusinessException(ErrorCode.SELLER_NOT_FOUND)));

            // when & then
            webTestClient.put()
                    .uri("/api/v1/sellers/me")
                    .header("Authorization", validToken)
                    .attribute("memberId", 100L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(updateRequest)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.error.code").isEqualTo("S001");
        }

        @Test
        @DisplayName("[실패] 서버 오류 발생")
        void updateSeller_fail_serverError() {
            // given
            given(sellerService.updateSeller(eq(100L), any(SellerUpdateRequest.class)))
                    .willReturn(Mono.error(new RuntimeException("Internal error")));

            // when & then
            webTestClient.put()
                    .uri("/api/v1/sellers/me")
                    .header("Authorization", validToken)
                    .attribute("memberId", 100L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(updateRequest)
                    .exchange()
                    .expectStatus().is5xxServerError();
        }
    }

    // ========== 판매자 탈퇴 테스트 ==========

    @Nested
    @DisplayName("DELETE /api/v1/sellers/me - 판매자 탈퇴")
    class WithdrawSeller {

        @Test
        @DisplayName("[성공] 판매자 탈퇴가 완료된다")
        void withdrawSeller_success() {
            // given
            given(sellerService.withdrawSeller(100L))
                    .willReturn(Mono.empty());

            // when & then
            webTestClient.delete()
                    .uri("/api/v1/sellers/me")
                    .header("Authorization", validToken)
                    .attribute("memberId", 100L)
                    .exchange()
                    .expectStatus().isNoContent()
                    .consumeWith(document("seller/withdraw-success",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint())
                    ));
        }

        @Test
        @DisplayName("[실패] 판매자가 아닌 경우 탈퇴 실패")
        void withdrawSeller_fail_notSeller() {
            // given
            given(sellerService.withdrawSeller(100L))
                    .willReturn(Mono.error(new BusinessException(ErrorCode.SELLER_NOT_FOUND)));

            // when & then
            webTestClient.delete()
                    .uri("/api/v1/sellers/me")
                    .header("Authorization", validToken)
                    .attribute("memberId", 100L)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.error.code").isEqualTo("S001");
        }

        @Test
        @DisplayName("[실패] 서버 오류 발생")
        void withdrawSeller_fail_serverError() {
            // given
            given(sellerService.withdrawSeller(100L))
                    .willReturn(Mono.error(new RuntimeException("Internal error")));

            // when & then
            webTestClient.delete()
                    .uri("/api/v1/sellers/me")
                    .header("Authorization", validToken)
                    .attribute("memberId", 100L)
                    .exchange()
                    .expectStatus().is5xxServerError();
        }
    }

    // ========== 판매자 여부 확인 테스트 ==========

    @Nested
    @DisplayName("GET /api/v1/sellers/check - 판매자 여부 확인")
    class CheckSeller {

        @Test
        @DisplayName("[성공] 판매자인 경우 true를 반환한다")
        void checkSeller_success_isSeller() {
            // given
            given(sellerService.isSeller(100L))
                    .willReturn(Mono.just(true));

            // when & then
            webTestClient.get()
                    .uri("/api/v1/sellers/check")
                    .header("Authorization", validToken)
                    .attribute("memberId", 100L)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.message").isEqualTo("판매자입니다.")
                    .jsonPath("$.data").isEqualTo(true)
                    .consumeWith(document("seller/check-success-seller",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            responseFields(
                                    fieldWithPath("success").description("요청 성공 여부"),
                                    fieldWithPath("message").description("응답 메시지"),
                                    fieldWithPath("data").description("판매자 여부 (true/false)")
                            )
                    ));
        }

        @Test
        @DisplayName("[성공] 판매자가 아닌 경우 false를 반환한다")
        void checkSeller_success_notSeller() {
            // given
            given(sellerService.isSeller(100L))
                    .willReturn(Mono.just(false));

            // when & then
            webTestClient.get()
                    .uri("/api/v1/sellers/check")
                    .header("Authorization", validToken)
                    .attribute("memberId", 100L)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.message").isEqualTo("판매자가 아닙니다.")
                    .jsonPath("$.data").isEqualTo(false);
        }

        @Test
        @DisplayName("[실패] 서버 오류 발생")
        void checkSeller_fail_serverError() {
            // given
            given(sellerService.isSeller(100L))
                    .willReturn(Mono.error(new RuntimeException("Internal error")));

            // when & then
            webTestClient.get()
                    .uri("/api/v1/sellers/check")
                    .header("Authorization", validToken)
                    .attribute("memberId", 100L)
                    .exchange()
                    .expectStatus().is5xxServerError();
        }
    }
}
