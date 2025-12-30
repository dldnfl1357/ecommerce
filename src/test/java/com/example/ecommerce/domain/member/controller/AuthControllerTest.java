package com.example.ecommerce.domain.member.controller;

import com.example.ecommerce.domain.member.dto.request.LoginRequest;
import com.example.ecommerce.domain.member.dto.request.SignupRequest;
import com.example.ecommerce.domain.member.dto.response.MemberResponse;
import com.example.ecommerce.domain.member.dto.response.TokenResponse;
import com.example.ecommerce.domain.member.entity.MemberGrade;
import com.example.ecommerce.domain.member.entity.MemberStatus;
import com.example.ecommerce.domain.member.service.AuthService;
import com.example.ecommerce.domain.member.service.MemberService;
import com.example.ecommerce.global.exception.BusinessException;
import com.example.ecommerce.global.exception.ErrorCode;
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
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@WebFluxTest(AuthController.class)
@AutoConfigureRestDocs
@DisplayName("AuthController 테스트")
class AuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private MemberService memberService;

    @MockBean
    private AuthService authService;

    // ========== 회원가입 테스트 ==========

    @Test
    @DisplayName("[성공] 회원가입 - 정상적으로 회원가입이 완료된다")
    void signup_success() {
        // given
        SignupRequest request = SignupRequest.builder()
            .email("test@example.com")
            .password("Test1234!@")
            .name("홍길동")
            .phone("010-1234-5678")
            .build();

        MemberResponse response = MemberResponse.builder()
            .id(1L)
            .email("test@example.com")
            .maskedEmail("tes***@example.com")
            .name("홍길동")
            .phone("01012345678")
            .grade(MemberGrade.BRONZE)
            .gradeDisplayName("브론즈")
            .point(0)
            .status(MemberStatus.ACTIVE)
            .statusDisplayName("활성")
            .rocketWowActive(false)
            .createdAt(LocalDateTime.now())
            .build();

        given(memberService.signup(any(SignupRequest.class)))
            .willReturn(Mono.just(response));

        // when & then
        webTestClient.post()
            .uri("/api/v1/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.id").isEqualTo(1)
            .jsonPath("$.data.email").isEqualTo("test@example.com")
            .jsonPath("$.data.grade").isEqualTo("BRONZE")
            .jsonPath("$.message").isEqualTo("회원 가입이 완료되었습니다.")
            .consumeWith(document("auth/signup-success",
                requestFields(
                    fieldWithPath("email").type(JsonFieldType.STRING)
                        .description("이메일 (필수, 이메일 형식)"),
                    fieldWithPath("password").type(JsonFieldType.STRING)
                        .description("비밀번호 (필수, 8-20자, 영문+숫자+특수문자)"),
                    fieldWithPath("name").type(JsonFieldType.STRING)
                        .description("이름 (필수, 2-50자)"),
                    fieldWithPath("phone").type(JsonFieldType.STRING)
                        .description("전화번호 (필수, 010-XXXX-XXXX 형식)")
                ),
                responseFields(
                    fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                        .description("성공 여부"),
                    fieldWithPath("message").type(JsonFieldType.STRING)
                        .description("응답 메시지"),
                    fieldWithPath("data").type(JsonFieldType.OBJECT)
                        .description("응답 데이터"),
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
                        .description("회원 등급 (BRONZE, SILVER, GOLD, VIP)"),
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
    @DisplayName("[실패] 회원가입 - 이메일 중복")
    void signup_fail_duplicateEmail() {
        // given
        SignupRequest request = SignupRequest.builder()
            .email("duplicate@example.com")
            .password("Test1234!@")
            .name("홍길동")
            .phone("010-1234-5678")
            .build();

        given(memberService.signup(any(SignupRequest.class)))
            .willReturn(Mono.error(new BusinessException(ErrorCode.DUPLICATE_EMAIL)));

        // when & then
        webTestClient.post()
            .uri("/api/v1/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isEqualTo(409)  // CONFLICT
            .expectBody()
            .jsonPath("$.success").isEqualTo(false);
    }

    @Test
    @DisplayName("[실패] 회원가입 - 전화번호 중복")
    void signup_fail_duplicatePhone() {
        // given
        SignupRequest request = SignupRequest.builder()
            .email("test@example.com")
            .password("Test1234!@")
            .name("홍길동")
            .phone("010-9999-9999")
            .build();

        given(memberService.signup(any(SignupRequest.class)))
            .willReturn(Mono.error(new BusinessException(ErrorCode.DUPLICATE_PHONE)));

        // when & then
        webTestClient.post()
            .uri("/api/v1/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isEqualTo(409);
    }

    @Test
    @DisplayName("[실패] 회원가입 - 잘못된 이메일 형식")
    void signup_fail_invalidEmailFormat() {
        // given
        SignupRequest request = SignupRequest.builder()
            .email("invalid-email")  // @ 없음
            .password("Test1234!@")
            .name("홍길동")
            .phone("010-1234-5678")
            .build();

        // when & then
        webTestClient.post()
            .uri("/api/v1/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("[실패] 회원가입 - 비밀번호 형식 오류")
    void signup_fail_invalidPasswordFormat() {
        // given
        SignupRequest request = SignupRequest.builder()
            .email("test@example.com")
            .password("weak")  // 너무 짧고 특수문자 없음
            .name("홍길동")
            .phone("010-1234-5678")
            .build();

        // when & then
        webTestClient.post()
            .uri("/api/v1/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("[실패] 회원가입 - 필수 필드 누락 (이름 없음)")
    void signup_fail_missingRequiredField() {
        // given
        SignupRequest request = SignupRequest.builder()
            .email("test@example.com")
            .password("Test1234!@")
            // name 누락
            .phone("010-1234-5678")
            .build();

        // when & then
        webTestClient.post()
            .uri("/api/v1/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest();
    }

    // ========== 로그인 테스트 ==========

    @Test
    @DisplayName("[성공] 로그인 - JWT 토큰 발급")
    void login_success() {
        // given
        LoginRequest request = LoginRequest.builder()
            .email("test@example.com")
            .password("Test1234!@")
            .build();

        TokenResponse response = TokenResponse.of(
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh...",
            3600L
        );

        given(authService.login(any(LoginRequest.class)))
            .willReturn(Mono.just(response));

        // when & then
        webTestClient.post()
            .uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.accessToken").exists()
            .jsonPath("$.data.refreshToken").exists()
            .jsonPath("$.data.tokenType").isEqualTo("Bearer")
            .jsonPath("$.data.expiresIn").isEqualTo(3600);
    }

    @Test
    @DisplayName("[실패] 로그인 - 잘못된 비밀번호")
    void login_fail_invalidPassword() {
        // given
        LoginRequest request = LoginRequest.builder()
            .email("test@example.com")
            .password("WrongPassword123!@")
            .build();

        given(authService.login(any(LoginRequest.class)))
            .willReturn(Mono.error(new BusinessException(ErrorCode.INVALID_PASSWORD)));

        // when & then
        webTestClient.post()
            .uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("[실패] 로그인 - 존재하지 않는 이메일")
    void login_fail_memberNotFound() {
        // given
        LoginRequest request = LoginRequest.builder()
            .email("notfound@example.com")
            .password("Test1234!@")
            .build();

        given(authService.login(any(LoginRequest.class)))
            .willReturn(Mono.error(new BusinessException(ErrorCode.INVALID_CREDENTIALS)));

        // when & then
        webTestClient.post()
            .uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("[실패] 로그인 - 휴면 계정")
    void login_fail_dormantMember() {
        // given
        LoginRequest request = LoginRequest.builder()
            .email("dormant@example.com")
            .password("Test1234!@")
            .build();

        given(authService.login(any(LoginRequest.class)))
            .willReturn(Mono.error(new BusinessException(ErrorCode.DORMANT_MEMBER)));

        // when & then
        webTestClient.post()
            .uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("[실패] 로그인 - 탈퇴한 회원")
    void login_fail_withdrawnMember() {
        // given
        LoginRequest request = LoginRequest.builder()
            .email("withdrawn@example.com")
            .password("Test1234!@")
            .build();

        given(authService.login(any(LoginRequest.class)))
            .willReturn(Mono.error(new BusinessException(ErrorCode.WITHDRAWN_MEMBER)));

        // when & then
        webTestClient.post()
            .uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isForbidden();
    }
}
