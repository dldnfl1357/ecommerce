package com.example.ecommerce.domain.member.service;

import com.example.ecommerce.domain.member.dto.request.MemberUpdateRequest;
import com.example.ecommerce.domain.member.dto.request.SignupRequest;
import com.example.ecommerce.domain.member.dto.response.MemberResponse;
import com.example.ecommerce.domain.member.entity.Member;
import com.example.ecommerce.domain.member.entity.MemberGrade;
import com.example.ecommerce.domain.member.entity.MemberStatus;
import com.example.ecommerce.domain.member.repository.MemberRepository;
import com.example.ecommerce.global.exception.BusinessException;
import com.example.ecommerce.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 테스트")
class MemberServiceTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private Member testMember;
    private SignupRequest signupRequest;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
            .email("test@example.com")
            .password("encodedPassword123")
            .name("홍길동")
            .phone("01012345678")
            .build();

        signupRequest = SignupRequest.builder()
            .email("test@example.com")
            .password("Test1234!@")
            .name("홍길동")
            .phone("010-1234-5678")
            .build();
    }

    // ========== 회원가입 테스트 ==========

    @Test
    @DisplayName("[성공] 회원가입 - 정상적으로 회원가입이 완료된다")
    void signup_success() {
        // given
        given(memberRepository.existsByEmail(anyString()))
            .willReturn(Mono.just(false));
        given(memberRepository.existsByPhone(anyString()))
            .willReturn(Mono.just(false));
        given(passwordEncoder.encode(anyString()))
            .willReturn("encodedPassword123");
        given(memberRepository.save(any(Member.class)))
            .willReturn(Mono.just(testMember));

        // when
        Mono<MemberResponse> result = memberService.signup(signupRequest);

        // then
        StepVerifier.create(result)
            .assertNext(response -> {
                assertThat(response).isNotNull();
                assertThat(response.getEmail()).isEqualTo("test@example.com");
                assertThat(response.getName()).isEqualTo("홍길동");
                assertThat(response.getGrade()).isEqualTo(MemberGrade.BRONZE);
                assertThat(response.getPoint()).isEqualTo(0);
            })
            .verifyComplete();

        verify(memberRepository, times(1)).existsByEmail("test@example.com");
        verify(memberRepository, times(1)).existsByPhone("01012345678");
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    @DisplayName("[실패] 회원가입 - 이메일 중복")
    void signup_fail_duplicateEmail() {
        // given
        given(memberRepository.existsByEmail(anyString()))
            .willReturn(Mono.just(true));

        // when
        Mono<MemberResponse> result = memberService.signup(signupRequest);

        // then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof BusinessException &&
                ((BusinessException) throwable).getErrorCode() == ErrorCode.DUPLICATE_EMAIL
            )
            .verify();

        verify(memberRepository, times(1)).existsByEmail("test@example.com");
        verify(memberRepository, times(0)).save(any(Member.class));
    }

    @Test
    @DisplayName("[실패] 회원가입 - 전화번호 중복")
    void signup_fail_duplicatePhone() {
        // given
        given(memberRepository.existsByEmail(anyString()))
            .willReturn(Mono.just(false));
        given(memberRepository.existsByPhone(anyString()))
            .willReturn(Mono.just(true));

        // when
        Mono<MemberResponse> result = memberService.signup(signupRequest);

        // then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof BusinessException &&
                ((BusinessException) throwable).getErrorCode() == ErrorCode.DUPLICATE_PHONE
            )
            .verify();

        verify(memberRepository, times(1)).existsByPhone("01012345678");
        verify(memberRepository, times(0)).save(any(Member.class));
    }

    @Test
    @DisplayName("[실패] 회원가입 - DB 저장 실패")
    void signup_fail_databaseError() {
        // given
        given(memberRepository.existsByEmail(anyString()))
            .willReturn(Mono.just(false));
        given(memberRepository.existsByPhone(anyString()))
            .willReturn(Mono.just(false));
        given(passwordEncoder.encode(anyString()))
            .willReturn("encodedPassword123");
        given(memberRepository.save(any(Member.class)))
            .willReturn(Mono.error(new RuntimeException("Database connection failed")));

        // when
        Mono<MemberResponse> result = memberService.signup(signupRequest);

        // then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof BusinessException &&
                ((BusinessException) throwable).getErrorCode() == ErrorCode.INTERNAL_SERVER_ERROR
            )
            .verify();
    }

    @Test
    @DisplayName("[실패] 회원가입 - 이메일 검증 중 오류")
    void signup_fail_emailValidationError() {
        // given
        given(memberRepository.existsByEmail(anyString()))
            .willReturn(Mono.error(new RuntimeException("Database error")));

        // when
        Mono<MemberResponse> result = memberService.signup(signupRequest);

        // then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof BusinessException &&
                ((BusinessException) throwable).getErrorCode() == ErrorCode.INTERNAL_SERVER_ERROR
            )
            .verify();
    }

    @Test
    @DisplayName("[실패] 회원가입 - 전화번호 검증 중 오류")
    void signup_fail_phoneValidationError() {
        // given
        given(memberRepository.existsByEmail(anyString()))
            .willReturn(Mono.just(false));
        given(memberRepository.existsByPhone(anyString()))
            .willReturn(Mono.error(new RuntimeException("Database error")));

        // when
        Mono<MemberResponse> result = memberService.signup(signupRequest);

        // then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof BusinessException &&
                ((BusinessException) throwable).getErrorCode() == ErrorCode.INTERNAL_SERVER_ERROR
            )
            .verify();
    }

    // ========== 회원 조회 테스트 ==========

    @Test
    @DisplayName("[성공] 회원 조회 - ID로 회원 정보를 조회한다")
    void getMember_success() {
        // given
        given(memberRepository.findById(1L))
            .willReturn(Mono.just(testMember));

        // when
        Mono<MemberResponse> result = memberService.getMember(1L);

        // then
        StepVerifier.create(result)
            .assertNext(response -> {
                assertThat(response).isNotNull();
                assertThat(response.getEmail()).isEqualTo("test@example.com");
                assertThat(response.getName()).isEqualTo("홍길동");
            })
            .verifyComplete();

        verify(memberRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("[실패] 회원 조회 - 존재하지 않는 회원 ID")
    void getMember_fail_memberNotFound() {
        // given
        given(memberRepository.findById(999L))
            .willReturn(Mono.empty());

        // when
        Mono<MemberResponse> result = memberService.getMember(999L);

        // then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof BusinessException &&
                ((BusinessException) throwable).getErrorCode() == ErrorCode.MEMBER_NOT_FOUND
            )
            .verify();
    }

    @Test
    @DisplayName("[실패] 회원 조회 - DB 조회 중 오류")
    void getMember_fail_databaseError() {
        // given
        given(memberRepository.findById(1L))
            .willReturn(Mono.error(new RuntimeException("Database error")));

        // when
        Mono<MemberResponse> result = memberService.getMember(1L);

        // then
        StepVerifier.create(result)
            .expectError(RuntimeException.class)
            .verify();
    }

    // ========== 이메일로 회원 조회 테스트 ==========

    @Test
    @DisplayName("[성공] 이메일로 회원 조회")
    void getMemberByEmail_success() {
        // given
        given(memberRepository.findByEmail("test@example.com"))
            .willReturn(Mono.just(testMember));

        // when
        Mono<MemberResponse> result = memberService.getMemberByEmail("test@example.com");

        // then
        StepVerifier.create(result)
            .assertNext(response -> {
                assertThat(response.getEmail()).isEqualTo("test@example.com");
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("[실패] 이메일로 회원 조회 - 존재하지 않는 이메일")
    void getMemberByEmail_fail_notFound() {
        // given
        given(memberRepository.findByEmail("notfound@example.com"))
            .willReturn(Mono.empty());

        // when
        Mono<MemberResponse> result = memberService.getMemberByEmail("notfound@example.com");

        // then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof BusinessException &&
                ((BusinessException) throwable).getErrorCode() == ErrorCode.MEMBER_NOT_FOUND
            )
            .verify();
    }

    // ========== 회원 탈퇴 테스트 ==========

    @Test
    @DisplayName("[성공] 회원 탈퇴 - 정상적으로 회원 탈퇴가 완료된다")
    void withdraw_success() {
        // given
        given(memberRepository.findById(1L))
            .willReturn(Mono.just(testMember));
        given(memberRepository.save(any(Member.class)))
            .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // when
        Mono<Void> result = memberService.withdraw(1L);

        // then
        StepVerifier.create(result)
            .verifyComplete();

        verify(memberRepository, times(1)).findById(1L);
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    @DisplayName("[실패] 회원 탈퇴 - 존재하지 않는 회원")
    void withdraw_fail_memberNotFound() {
        // given
        given(memberRepository.findById(999L))
            .willReturn(Mono.empty());

        // when
        Mono<Void> result = memberService.withdraw(999L);

        // then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof BusinessException &&
                ((BusinessException) throwable).getErrorCode() == ErrorCode.MEMBER_NOT_FOUND
            )
            .verify();

        verify(memberRepository, times(0)).save(any(Member.class));
    }

    @Test
    @DisplayName("[실패] 회원 탈퇴 - DB 저장 실패")
    void withdraw_fail_saveError() {
        // given
        given(memberRepository.findById(1L))
            .willReturn(Mono.just(testMember));
        given(memberRepository.save(any(Member.class)))
            .willReturn(Mono.error(new RuntimeException("Save failed")));

        // when
        Mono<Void> result = memberService.withdraw(1L);

        // then
        StepVerifier.create(result)
            .expectError(RuntimeException.class)
            .verify();
    }

    // ========== 적립금 사용 테스트 ==========

    @Test
    @DisplayName("[성공] 적립금 사용 - 정상적으로 적립금이 차감된다")
    void usePoint_success() {
        // given
        Member memberWithPoint = Member.builder()
            .email("test@example.com")
            .password("password")
            .name("홍길동")
            .phone("01012345678")
            .build();
        memberWithPoint.earnPoint(10000); // 10,000원 적립금 보유

        given(memberRepository.findById(1L))
            .willReturn(Mono.just(memberWithPoint));
        given(memberRepository.save(any(Member.class)))
            .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // when
        Mono<Void> result = memberService.usePoint(1L, 5000);

        // then
        StepVerifier.create(result)
            .verifyComplete();

        verify(memberRepository, times(1)).findById(1L);
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    @DisplayName("[실패] 적립금 사용 - 존재하지 않는 회원")
    void usePoint_fail_memberNotFound() {
        // given
        given(memberRepository.findById(999L))
            .willReturn(Mono.empty());

        // when
        Mono<Void> result = memberService.usePoint(999L, 1000);

        // then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof BusinessException &&
                ((BusinessException) throwable).getErrorCode() == ErrorCode.MEMBER_NOT_FOUND
            )
            .verify();
    }

    @Test
    @DisplayName("[실패] 적립금 사용 - 최소 사용 금액 미만 (1,000원 미만)")
    void usePoint_fail_belowMinimum() {
        // given
        Member memberWithPoint = Member.builder()
            .email("test@example.com")
            .password("password")
            .name("홍길동")
            .phone("01012345678")
            .build();
        memberWithPoint.earnPoint(10000);

        given(memberRepository.findById(1L))
            .willReturn(Mono.just(memberWithPoint));

        // when
        Mono<Void> result = memberService.usePoint(1L, 500); // 500원 사용 시도

        // then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof BusinessException &&
                ((BusinessException) throwable).getErrorCode() == ErrorCode.INSUFFICIENT_POINT_MINIMUM
            )
            .verify();

        verify(memberRepository, times(0)).save(any(Member.class));
    }

    @Test
    @DisplayName("[실패] 적립금 사용 - 보유 적립금 부족")
    void usePoint_fail_insufficientPoint() {
        // given
        Member memberWithPoint = Member.builder()
            .email("test@example.com")
            .password("password")
            .name("홍길동")
            .phone("01012345678")
            .build();
        memberWithPoint.earnPoint(3000); // 3,000원만 보유

        given(memberRepository.findById(1L))
            .willReturn(Mono.just(memberWithPoint));

        // when
        Mono<Void> result = memberService.usePoint(1L, 5000); // 5,000원 사용 시도

        // then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof BusinessException &&
                ((BusinessException) throwable).getErrorCode() == ErrorCode.INSUFFICIENT_POINT
            )
            .verify();

        verify(memberRepository, times(0)).save(any(Member.class));
    }

    @Test
    @DisplayName("[실패] 적립금 사용 - 음수 금액")
    void usePoint_fail_negativeAmount() {
        // given
        Member memberWithPoint = Member.builder()
            .email("test@example.com")
            .password("password")
            .name("홍길동")
            .phone("01012345678")
            .build();
        memberWithPoint.earnPoint(10000);

        given(memberRepository.findById(1L))
            .willReturn(Mono.just(memberWithPoint));

        // when
        Mono<Void> result = memberService.usePoint(1L, -1000); // 음수 금액

        // then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof BusinessException &&
                ((BusinessException) throwable).getErrorCode() == ErrorCode.INSUFFICIENT_POINT_MINIMUM
            )
            .verify();
    }

    // ========== 적립금 적립 테스트 ==========

    @Test
    @DisplayName("[성공] 적립금 적립 - 정상적으로 적립금이 적립된다")
    void earnPoint_success() {
        // given
        given(memberRepository.findById(1L))
            .willReturn(Mono.just(testMember));
        given(memberRepository.save(any(Member.class)))
            .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // when
        Mono<Void> result = memberService.earnPoint(1L, 5000);

        // then
        StepVerifier.create(result)
            .verifyComplete();

        verify(memberRepository, times(1)).findById(1L);
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    @DisplayName("[실패] 적립금 적립 - 존재하지 않는 회원")
    void earnPoint_fail_memberNotFound() {
        // given
        given(memberRepository.findById(999L))
            .willReturn(Mono.empty());

        // when
        Mono<Void> result = memberService.earnPoint(999L, 1000);

        // then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof BusinessException &&
                ((BusinessException) throwable).getErrorCode() == ErrorCode.MEMBER_NOT_FOUND
            )
            .verify();
    }

    // ========== 로켓와우 구독 테스트 ==========

    @Test
    @DisplayName("[성공] 로켓와우 구독 - 정상적으로 로켓와우 구독이 완료된다")
    void subscribeRocketWow_success() {
        // given
        given(memberRepository.findById(1L))
            .willReturn(Mono.just(testMember));
        given(memberRepository.save(any(Member.class)))
            .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // when
        Mono<MemberResponse> result = memberService.subscribeRocketWow(1L);

        // then
        StepVerifier.create(result)
            .assertNext(response -> {
                assertThat(response).isNotNull();
                assertThat(response.getRocketWowActive()).isTrue();
            })
            .verifyComplete();

        verify(memberRepository, times(1)).findById(1L);
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    @DisplayName("[실패] 로켓와우 구독 - 존재하지 않는 회원")
    void subscribeRocketWow_fail_memberNotFound() {
        // given
        given(memberRepository.findById(999L))
            .willReturn(Mono.empty());

        // when
        Mono<MemberResponse> result = memberService.subscribeRocketWow(999L);

        // then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof BusinessException &&
                ((BusinessException) throwable).getErrorCode() == ErrorCode.MEMBER_NOT_FOUND
            )
            .verify();
    }

    @Test
    @DisplayName("[실패] 로켓와우 구독 - DB 저장 실패")
    void subscribeRocketWow_fail_saveError() {
        // given
        given(memberRepository.findById(1L))
            .willReturn(Mono.just(testMember));
        given(memberRepository.save(any(Member.class)))
            .willReturn(Mono.error(new RuntimeException("Save failed")));

        // when
        Mono<MemberResponse> result = memberService.subscribeRocketWow(1L);

        // then
        StepVerifier.create(result)
            .expectError(RuntimeException.class)
            .verify();
    }

    // ========== 로켓와우 취소 테스트 ==========

    @Test
    @DisplayName("[성공] 로켓와우 취소 - 정상적으로 로켓와우 구독이 취소된다")
    void cancelRocketWow_success() {
        // given
        testMember.subscribeRocketWow(); // 먼저 구독 상태로 만듦

        given(memberRepository.findById(1L))
            .willReturn(Mono.just(testMember));
        given(memberRepository.save(any(Member.class)))
            .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // when
        Mono<MemberResponse> result = memberService.cancelRocketWow(1L);

        // then
        StepVerifier.create(result)
            .assertNext(response -> {
                assertThat(response).isNotNull();
                assertThat(response.getRocketWowActive()).isFalse();
            })
            .verifyComplete();

        verify(memberRepository, times(1)).findById(1L);
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    @DisplayName("[실패] 로켓와우 취소 - 존재하지 않는 회원")
    void cancelRocketWow_fail_memberNotFound() {
        // given
        given(memberRepository.findById(999L))
            .willReturn(Mono.empty());

        // when
        Mono<MemberResponse> result = memberService.cancelRocketWow(999L);

        // then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof BusinessException &&
                ((BusinessException) throwable).getErrorCode() == ErrorCode.MEMBER_NOT_FOUND
            )
            .verify();
    }

    @Test
    @DisplayName("[실패] 로켓와우 취소 - DB 저장 실패")
    void cancelRocketWow_fail_saveError() {
        // given
        testMember.subscribeRocketWow();

        given(memberRepository.findById(1L))
            .willReturn(Mono.just(testMember));
        given(memberRepository.save(any(Member.class)))
            .willReturn(Mono.error(new RuntimeException("Save failed")));

        // when
        Mono<MemberResponse> result = memberService.cancelRocketWow(1L);

        // then
        StepVerifier.create(result)
            .expectError(RuntimeException.class)
            .verify();
    }

    // ========== 회원 등급 업데이트 테스트 ==========

    @Test
    @DisplayName("[성공] 회원 등급 업데이트 - 구매 금액에 따라 등급이 업그레이드된다")
    void updateGrade_success() {
        // given
        given(memberRepository.findById(1L))
            .willReturn(Mono.just(testMember));
        given(memberRepository.save(any(Member.class)))
            .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // when - 50만원 구매로 SILVER 등급으로 업그레이드
        Mono<Void> result = memberService.updateGrade(1L, 500_000);

        // then
        StepVerifier.create(result)
            .verifyComplete();

        verify(memberRepository, times(1)).findById(1L);
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    @DisplayName("[실패] 회원 등급 업데이트 - 존재하지 않는 회원")
    void updateGrade_fail_memberNotFound() {
        // given
        given(memberRepository.findById(999L))
            .willReturn(Mono.empty());

        // when
        Mono<Void> result = memberService.updateGrade(999L, 500_000);

        // then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof BusinessException &&
                ((BusinessException) throwable).getErrorCode() == ErrorCode.MEMBER_NOT_FOUND
            )
            .verify();
    }

    @Test
    @DisplayName("[성공] 회원 등급 업데이트 - 등급 변화 없음 (업그레이드 조건 미달)")
    void updateGrade_noUpgrade() {
        // given
        given(memberRepository.findById(1L))
            .willReturn(Mono.just(testMember));
        given(memberRepository.save(any(Member.class)))
            .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // when - 50,000원 구매 (BRONZE 유지)
        Mono<Void> result = memberService.updateGrade(1L, 50_000);

        // then
        StepVerifier.create(result)
            .verifyComplete();
    }
}
