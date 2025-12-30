package com.example.ecommerce.domain.member.controller;

import com.example.ecommerce.domain.member.dto.request.AddressCreateRequest;
import com.example.ecommerce.domain.member.dto.request.MemberUpdateRequest;
import com.example.ecommerce.domain.member.dto.response.AddressResponse;
import com.example.ecommerce.domain.member.dto.response.MemberResponse;
import com.example.ecommerce.domain.member.entity.Address;
import com.example.ecommerce.domain.member.entity.Member;
import com.example.ecommerce.domain.member.entity.MemberGrade;
import com.example.ecommerce.domain.member.entity.MemberStatus;
import com.example.ecommerce.domain.member.service.AddressService;
import com.example.ecommerce.domain.member.service.MemberService;
import com.example.ecommerce.global.exception.BusinessException;
import com.example.ecommerce.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@WebFluxTest(MemberController.class)
@AutoConfigureRestDocs
@DisplayName("MemberController 테스트")
class MemberControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private MemberService memberService;

    @MockBean
    private AddressService addressService;

    private MemberResponse memberResponse;
    private AddressResponse addressResponse;

    @BeforeEach
    void setUp() {
        memberResponse = MemberResponse.builder()
            .id(1L)
            .email("test@example.com")
            .maskedEmail("tes***@example.com")
            .name("홍길동")
            .phone("01012345678")
            .grade(MemberGrade.BRONZE)
            .gradeDisplayName("브론즈")
            .point(1000)
            .status(MemberStatus.ACTIVE)
            .statusDisplayName("활성")
            .rocketWowActive(false)
            .createdAt(LocalDateTime.now())
            .build();

        addressResponse = AddressResponse.builder()
            .id(1L)
            .memberId(1L)
            .name("집")
            .recipient("홍길동")
            .phone("01012345678")
            .zipCode("12345")
            .address("서울시 강남구 테헤란로 123")
            .addressDetail("101호")
            .isDefault(true)
            .deliveryRequest("부재 시 문 앞에 놓아주세요")
            .build();
    }

    // ========== 내 정보 조회 테스트 ==========

    @Test
    @DisplayName("[성공] 내 정보 조회")
    void getMyInfo_success() {
        // given
        given(memberService.getMember(1L))
            .willReturn(Mono.just(memberResponse));

        // when & then
        webTestClient.get()
            .uri("/api/v1/members/me")
            .attribute("memberId", 1L)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.id").isEqualTo(1)
            .jsonPath("$.data.email").isEqualTo("test@example.com")
            .jsonPath("$.data.name").isEqualTo("홍길동")
            .consumeWith(document("member/get-my-info-success",
                responseFields(
                    fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                        .description("성공 여부"),
                    fieldWithPath("message").type(JsonFieldType.STRING)
                        .optional()
                        .description("응답 메시지"),
                    fieldWithPath("data").type(JsonFieldType.OBJECT)
                        .description("회원 정보"),
                    fieldWithPath("data.id").type(JsonFieldType.NUMBER)
                        .description("회원 ID"),
                    fieldWithPath("data.email").type(JsonFieldType.STRING)
                        .description("이메일"),
                    fieldWithPath("data.maskedEmail").type(JsonFieldType.STRING)
                        .description("마스킹된 이메일"),
                    fieldWithPath("data.name").type(JsonFieldType.STRING)
                        .description("이름"),
                    fieldWithPath("data.phone").type(JsonFieldType.STRING)
                        .description("전화번호"),
                    fieldWithPath("data.grade").type(JsonFieldType.STRING)
                        .description("회원 등급"),
                    fieldWithPath("data.gradeDisplayName").type(JsonFieldType.STRING)
                        .description("회원 등급 표시명"),
                    fieldWithPath("data.point").type(JsonFieldType.NUMBER)
                        .description("적립금"),
                    fieldWithPath("data.status").type(JsonFieldType.STRING)
                        .description("회원 상태"),
                    fieldWithPath("data.statusDisplayName").type(JsonFieldType.STRING)
                        .description("회원 상태 표시명"),
                    fieldWithPath("data.rocketWowActive").type(JsonFieldType.BOOLEAN)
                        .description("로켓와우 활성 여부"),
                    fieldWithPath("data.rocketWowExpiresAt").type(JsonFieldType.STRING)
                        .optional()
                        .description("로켓와우 만료일"),
                    fieldWithPath("data.lastLoginAt").type(JsonFieldType.STRING)
                        .optional()
                        .description("마지막 로그인 일시"),
                    fieldWithPath("data.createdAt").type(JsonFieldType.STRING)
                        .description("가입일")
                )
            ));
    }

    @Test
    @DisplayName("[실패] 내 정보 조회 - 존재하지 않는 회원")
    void getMyInfo_fail_memberNotFound() {
        // given
        given(memberService.getMember(999L))
            .willReturn(Mono.error(new BusinessException(ErrorCode.MEMBER_NOT_FOUND)));

        // when & then
        webTestClient.get()
            .uri("/api/v1/members/me")
            .attribute("memberId", 999L)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody()
            .jsonPath("$.success").isEqualTo(false);
    }

    // ========== 내 정보 수정 테스트 ==========

    @Test
    @DisplayName("[성공] 내 정보 수정")
    void updateMyInfo_success() {
        // given
        MemberUpdateRequest request = MemberUpdateRequest.builder()
            .name("김철수")
            .phone("010-9999-8888")
            .build();

        MemberResponse updatedResponse = MemberResponse.builder()
            .id(1L)
            .email("test@example.com")
            .maskedEmail("tes***@example.com")
            .name("김철수")
            .phone("01099998888")
            .grade(MemberGrade.BRONZE)
            .gradeDisplayName("브론즈")
            .point(1000)
            .status(MemberStatus.ACTIVE)
            .statusDisplayName("활성")
            .rocketWowActive(false)
            .createdAt(LocalDateTime.now())
            .build();

        given(memberService.updateMember(anyLong(), any(MemberUpdateRequest.class)))
            .willReturn(Mono.just(updatedResponse));

        // when & then
        webTestClient.put()
            .uri("/api/v1/members/me")
            .attribute("memberId", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.name").isEqualTo("김철수")
            .jsonPath("$.message").isEqualTo("회원 정보가 수정되었습니다.");
    }

    @Test
    @DisplayName("[실패] 내 정보 수정 - 존재하지 않는 회원")
    void updateMyInfo_fail_memberNotFound() {
        // given
        MemberUpdateRequest request = MemberUpdateRequest.builder()
            .name("김철수")
            .build();

        given(memberService.updateMember(anyLong(), any(MemberUpdateRequest.class)))
            .willReturn(Mono.error(new BusinessException(ErrorCode.MEMBER_NOT_FOUND)));

        // when & then
        webTestClient.put()
            .uri("/api/v1/members/me")
            .attribute("memberId", 999L)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("[실패] 내 정보 수정 - 중복된 전화번호")
    void updateMyInfo_fail_duplicatePhone() {
        // given
        MemberUpdateRequest request = MemberUpdateRequest.builder()
            .phone("010-1111-2222")
            .build();

        given(memberService.updateMember(anyLong(), any(MemberUpdateRequest.class)))
            .willReturn(Mono.error(new BusinessException(ErrorCode.DUPLICATE_PHONE)));

        // when & then
        webTestClient.put()
            .uri("/api/v1/members/me")
            .attribute("memberId", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isEqualTo(409);
    }

    // ========== 회원 탈퇴 테스트 ==========

    @Test
    @DisplayName("[성공] 회원 탈퇴")
    void withdraw_success() {
        // given
        given(memberService.withdraw(1L))
            .willReturn(Mono.empty());

        // when & then
        webTestClient.delete()
            .uri("/api/v1/members/me")
            .attribute("memberId", 1L)
            .exchange()
            .expectStatus().isNoContent()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.message").isEqualTo("회원 탈퇴가 완료되었습니다.");
    }

    @Test
    @DisplayName("[실패] 회원 탈퇴 - 존재하지 않는 회원")
    void withdraw_fail_memberNotFound() {
        // given
        given(memberService.withdraw(999L))
            .willReturn(Mono.error(new BusinessException(ErrorCode.MEMBER_NOT_FOUND)));

        // when & then
        webTestClient.delete()
            .uri("/api/v1/members/me")
            .attribute("memberId", 999L)
            .exchange()
            .expectStatus().isNotFound();
    }

    // ========== 로켓와우 구독 테스트 ==========

    @Test
    @DisplayName("[성공] 로켓와우 구독")
    void subscribeRocketWow_success() {
        // given
        MemberResponse rocketWowResponse = MemberResponse.builder()
            .id(1L)
            .email("test@example.com")
            .maskedEmail("tes***@example.com")
            .name("홍길동")
            .phone("01012345678")
            .grade(MemberGrade.BRONZE)
            .gradeDisplayName("브론즈")
            .point(1000)
            .status(MemberStatus.ACTIVE)
            .statusDisplayName("활성")
            .rocketWowActive(true)
            .rocketWowExpiresAt(LocalDateTime.now().plusMonths(1))
            .createdAt(LocalDateTime.now())
            .build();

        given(memberService.subscribeRocketWow(1L))
            .willReturn(Mono.just(rocketWowResponse));

        // when & then
        webTestClient.post()
            .uri("/api/v1/members/me/rocket-wow")
            .attribute("memberId", 1L)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.rocketWowActive").isEqualTo(true)
            .jsonPath("$.message").isEqualTo("로켓와우 구독이 완료되었습니다.")
            .consumeWith(document("member/rocket-wow-subscribe-success",
                responseFields(
                    fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                        .description("성공 여부"),
                    fieldWithPath("message").type(JsonFieldType.STRING)
                        .description("응답 메시지"),
                    fieldWithPath("data").type(JsonFieldType.OBJECT)
                        .description("회원 정보"),
                    fieldWithPath("data.id").type(JsonFieldType.NUMBER)
                        .description("회원 ID"),
                    fieldWithPath("data.email").type(JsonFieldType.STRING)
                        .description("이메일"),
                    fieldWithPath("data.maskedEmail").type(JsonFieldType.STRING)
                        .description("마스킹된 이메일"),
                    fieldWithPath("data.name").type(JsonFieldType.STRING)
                        .description("이름"),
                    fieldWithPath("data.phone").type(JsonFieldType.STRING)
                        .description("전화번호"),
                    fieldWithPath("data.grade").type(JsonFieldType.STRING)
                        .description("회원 등급"),
                    fieldWithPath("data.gradeDisplayName").type(JsonFieldType.STRING)
                        .description("회원 등급 표시명"),
                    fieldWithPath("data.point").type(JsonFieldType.NUMBER)
                        .description("적립금"),
                    fieldWithPath("data.status").type(JsonFieldType.STRING)
                        .description("회원 상태"),
                    fieldWithPath("data.statusDisplayName").type(JsonFieldType.STRING)
                        .description("회원 상태 표시명"),
                    fieldWithPath("data.rocketWowActive").type(JsonFieldType.BOOLEAN)
                        .description("로켓와우 활성 여부"),
                    fieldWithPath("data.rocketWowExpiresAt").type(JsonFieldType.STRING)
                        .optional()
                        .description("로켓와우 만료일"),
                    fieldWithPath("data.lastLoginAt").type(JsonFieldType.STRING)
                        .optional()
                        .description("마지막 로그인 일시"),
                    fieldWithPath("data.createdAt").type(JsonFieldType.STRING)
                        .description("가입일")
                )
            ));
    }

    @Test
    @DisplayName("[실패] 로켓와우 구독 - 존재하지 않는 회원")
    void subscribeRocketWow_fail_memberNotFound() {
        // given
        given(memberService.subscribeRocketWow(999L))
            .willReturn(Mono.error(new BusinessException(ErrorCode.MEMBER_NOT_FOUND)));

        // when & then
        webTestClient.post()
            .uri("/api/v1/members/me/rocket-wow")
            .attribute("memberId", 999L)
            .exchange()
            .expectStatus().isNotFound();
    }

    // ========== 로켓와우 취소 테스트 ==========

    @Test
    @DisplayName("[성공] 로켓와우 취소")
    void cancelRocketWow_success() {
        // given
        given(memberService.cancelRocketWow(1L))
            .willReturn(Mono.just(memberResponse));

        // when & then
        webTestClient.delete()
            .uri("/api/v1/members/me/rocket-wow")
            .attribute("memberId", 1L)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.rocketWowActive").isEqualTo(false)
            .jsonPath("$.message").isEqualTo("로켓와우 구독이 취소되었습니다.");
    }

    @Test
    @DisplayName("[실패] 로켓와우 취소 - 존재하지 않는 회원")
    void cancelRocketWow_fail_memberNotFound() {
        // given
        given(memberService.cancelRocketWow(999L))
            .willReturn(Mono.error(new BusinessException(ErrorCode.MEMBER_NOT_FOUND)));

        // when & then
        webTestClient.delete()
            .uri("/api/v1/members/me/rocket-wow")
            .attribute("memberId", 999L)
            .exchange()
            .expectStatus().isNotFound();
    }

    // ========== 배송지 추가 테스트 ==========

    @Test
    @DisplayName("[성공] 배송지 추가")
    void createAddress_success() {
        // given
        AddressCreateRequest request = AddressCreateRequest.builder()
            .name("집")
            .recipient("홍길동")
            .phone("010-1234-5678")
            .zipCode("12345")
            .address("서울시 강남구 테헤란로 123")
            .addressDetail("101호")
            .isDefault(false)
            .deliveryRequest("부재 시 문 앞에 놓아주세요")
            .build();

        given(addressService.createAddress(anyLong(), any(AddressCreateRequest.class)))
            .willReturn(Mono.just(addressResponse));

        // when & then
        webTestClient.post()
            .uri("/api/v1/members/me/addresses")
            .attribute("memberId", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.name").isEqualTo("집")
            .jsonPath("$.data.recipient").isEqualTo("홍길동")
            .jsonPath("$.message").isEqualTo("배송지가 추가되었습니다.")
            .consumeWith(document("member/address-create-success",
                requestFields(
                    fieldWithPath("name").type(JsonFieldType.STRING)
                        .description("배송지명 (필수)"),
                    fieldWithPath("recipient").type(JsonFieldType.STRING)
                        .description("수령인 (필수)"),
                    fieldWithPath("phone").type(JsonFieldType.STRING)
                        .description("전화번호 (필수, 010-XXXX-XXXX 형식)"),
                    fieldWithPath("zipCode").type(JsonFieldType.STRING)
                        .description("우편번호 (필수, 5자리 숫자)"),
                    fieldWithPath("address").type(JsonFieldType.STRING)
                        .description("주소 (필수)"),
                    fieldWithPath("addressDetail").type(JsonFieldType.STRING)
                        .optional()
                        .description("상세주소"),
                    fieldWithPath("isDefault").type(JsonFieldType.BOOLEAN)
                        .optional()
                        .description("기본 배송지 여부"),
                    fieldWithPath("deliveryRequest").type(JsonFieldType.STRING)
                        .optional()
                        .description("배송 요청사항")
                ),
                responseFields(
                    fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                        .description("성공 여부"),
                    fieldWithPath("message").type(JsonFieldType.STRING)
                        .description("응답 메시지"),
                    fieldWithPath("data").type(JsonFieldType.OBJECT)
                        .description("배송지 정보"),
                    fieldWithPath("data.id").type(JsonFieldType.NUMBER)
                        .description("배송지 ID"),
                    fieldWithPath("data.memberId").type(JsonFieldType.NUMBER)
                        .description("회원 ID"),
                    fieldWithPath("data.name").type(JsonFieldType.STRING)
                        .description("배송지명"),
                    fieldWithPath("data.recipient").type(JsonFieldType.STRING)
                        .description("수령인"),
                    fieldWithPath("data.phone").type(JsonFieldType.STRING)
                        .description("전화번호"),
                    fieldWithPath("data.zipCode").type(JsonFieldType.STRING)
                        .description("우편번호"),
                    fieldWithPath("data.address").type(JsonFieldType.STRING)
                        .description("주소"),
                    fieldWithPath("data.addressDetail").type(JsonFieldType.STRING)
                        .optional()
                        .description("상세주소"),
                    fieldWithPath("data.isDefault").type(JsonFieldType.BOOLEAN)
                        .description("기본 배송지 여부"),
                    fieldWithPath("data.deliveryRequest").type(JsonFieldType.STRING)
                        .optional()
                        .description("배송 요청사항")
                )
            ));
    }

    @Test
    @DisplayName("[실패] 배송지 추가 - 최대 배송지 개수 초과")
    void createAddress_fail_maxCountExceeded() {
        // given
        AddressCreateRequest request = AddressCreateRequest.builder()
            .name("집")
            .recipient("홍길동")
            .phone("010-1234-5678")
            .zipCode("12345")
            .address("서울시 강남구")
            .build();

        given(addressService.createAddress(anyLong(), any(AddressCreateRequest.class)))
            .willReturn(Mono.error(new BusinessException(ErrorCode.MAX_ADDRESS_COUNT_EXCEEDED)));

        // when & then
        webTestClient.post()
            .uri("/api/v1/members/me/addresses")
            .attribute("memberId", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.success").isEqualTo(false);
    }

    @Test
    @DisplayName("[실패] 배송지 추가 - 필수 필드 누락")
    void createAddress_fail_missingRequiredField() {
        // given
        AddressCreateRequest request = AddressCreateRequest.builder()
            .name("집")
            // recipient 누락
            .phone("010-1234-5678")
            .zipCode("12345")
            .address("서울시 강남구")
            .build();

        // when & then
        webTestClient.post()
            .uri("/api/v1/members/me/addresses")
            .attribute("memberId", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("[실패] 배송지 추가 - 잘못된 전화번호 형식")
    void createAddress_fail_invalidPhoneFormat() {
        // given
        AddressCreateRequest request = AddressCreateRequest.builder()
            .name("집")
            .recipient("홍길동")
            .phone("invalid-phone")
            .zipCode("12345")
            .address("서울시 강남구")
            .build();

        // when & then
        webTestClient.post()
            .uri("/api/v1/members/me/addresses")
            .attribute("memberId", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("[실패] 배송지 추가 - 잘못된 우편번호 형식")
    void createAddress_fail_invalidZipCodeFormat() {
        // given
        AddressCreateRequest request = AddressCreateRequest.builder()
            .name("집")
            .recipient("홍길동")
            .phone("010-1234-5678")
            .zipCode("123")  // 5자리가 아님
            .address("서울시 강남구")
            .build();

        // when & then
        webTestClient.post()
            .uri("/api/v1/members/me/addresses")
            .attribute("memberId", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest();
    }

    // ========== 배송지 목록 조회 테스트 ==========

    @Test
    @DisplayName("[성공] 배송지 목록 조회")
    void getAddresses_success() {
        // given
        given(addressService.getAddresses(1L))
            .willReturn(Flux.just(addressResponse));

        // when & then
        webTestClient.get()
            .uri("/api/v1/members/me/addresses")
            .attribute("memberId", 1L)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data").isArray();
    }

    @Test
    @DisplayName("[성공] 배송지 목록 조회 - 빈 목록")
    void getAddresses_success_emptyList() {
        // given
        given(addressService.getAddresses(1L))
            .willReturn(Flux.empty());

        // when & then
        webTestClient.get()
            .uri("/api/v1/members/me/addresses")
            .attribute("memberId", 1L)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true);
    }

    // ========== 기본 배송지 조회 테스트 ==========

    @Test
    @DisplayName("[성공] 기본 배송지 조회")
    void getDefaultAddress_success() {
        // given
        given(addressService.getDefaultAddress(1L))
            .willReturn(Mono.just(addressResponse));

        // when & then
        webTestClient.get()
            .uri("/api/v1/members/me/addresses/default")
            .attribute("memberId", 1L)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.isDefault").isEqualTo(true);
    }

    @Test
    @DisplayName("[실패] 기본 배송지 조회 - 기본 배송지 없음")
    void getDefaultAddress_fail_notFound() {
        // given
        given(addressService.getDefaultAddress(1L))
            .willReturn(Mono.error(new BusinessException(ErrorCode.DEFAULT_ADDRESS_NOT_FOUND)));

        // when & then
        webTestClient.get()
            .uri("/api/v1/members/me/addresses/default")
            .attribute("memberId", 1L)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody()
            .jsonPath("$.success").isEqualTo(false);
    }

    // ========== 배송지 삭제 테스트 ==========

    @Test
    @DisplayName("[성공] 배송지 삭제")
    void deleteAddress_success() {
        // given
        given(addressService.deleteAddress(1L, 1L))
            .willReturn(Mono.empty());

        // when & then
        webTestClient.delete()
            .uri("/api/v1/members/me/addresses/1")
            .attribute("memberId", 1L)
            .exchange()
            .expectStatus().isNoContent()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.message").isEqualTo("배송지가 삭제되었습니다.");
    }

    @Test
    @DisplayName("[실패] 배송지 삭제 - 존재하지 않는 배송지")
    void deleteAddress_fail_notFound() {
        // given
        given(addressService.deleteAddress(999L, 1L))
            .willReturn(Mono.error(new BusinessException(ErrorCode.ADDRESS_NOT_FOUND)));

        // when & then
        webTestClient.delete()
            .uri("/api/v1/members/me/addresses/999")
            .attribute("memberId", 1L)
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("[실패] 배송지 삭제 - 다른 회원의 배송지")
    void deleteAddress_fail_unauthorizedAccess() {
        // given
        given(addressService.deleteAddress(1L, 2L))
            .willReturn(Mono.error(new BusinessException(ErrorCode.UNAUTHORIZED_ADDRESS_ACCESS)));

        // when & then
        webTestClient.delete()
            .uri("/api/v1/members/me/addresses/1")
            .attribute("memberId", 2L)
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("[실패] 배송지 삭제 - 기본 배송지는 삭제 불가")
    void deleteAddress_fail_cannotDeleteDefaultAddress() {
        // given
        given(addressService.deleteAddress(1L, 1L))
            .willReturn(Mono.error(new BusinessException(ErrorCode.CANNOT_DELETE_DEFAULT_ADDRESS)));

        // when & then
        webTestClient.delete()
            .uri("/api/v1/members/me/addresses/1")
            .attribute("memberId", 1L)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.success").isEqualTo(false);
    }
}
