package com.example.ecommerce.domain.member.service;

import com.example.ecommerce.domain.member.dto.request.LoginRequest;
import com.example.ecommerce.domain.member.dto.response.TokenResponse;
import com.example.ecommerce.domain.member.entity.Member;
import com.example.ecommerce.domain.member.entity.MemberStatus;
import com.example.ecommerce.domain.member.repository.MemberRepository;
import com.example.ecommerce.global.auth.JwtTokenProvider;
import com.example.ecommerce.global.exception.BusinessException;
import com.example.ecommerce.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

    /**
     * 로그인 (함수형 스타일)
     */
    @Transactional
    public Mono<TokenResponse> login(LoginRequest request) {
        return memberRepository.findByEmail(request.getEmail())
            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVALID_CREDENTIALS)))
            .filterWhen(member -> validatePassword(request.getPassword(), member.getPassword()))
            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVALID_PASSWORD)))
            .flatMap(this::validateMemberStatus)
            .flatMap(this::updateLastLogin)
            .flatMap(this::generateTokens)
            .doOnSuccess(token -> log.info("로그인 성공: {}", request.getEmail()))
            .onErrorMap(this::mapAuthException);
    }

    /**
     * 토큰 갱신
     */
    public Mono<TokenResponse> refresh(String refreshToken) {
        return jwtTokenProvider.validateTokenReactive(refreshToken)
            .flatMap(isValid -> isValid
                ? jwtTokenProvider.getMemberIdReactive(refreshToken)
                : Mono.error(new BusinessException(ErrorCode.INVALID_TOKEN)))
            .flatMap(this::verifyRefreshToken)
            .flatMap(this::generateAccessToken)
            .doOnSuccess(token -> log.info("토큰 갱신 완료"))
            .onErrorMap(this::mapAuthException);
    }

    /**
     * 로그아웃
     */
    public Mono<Void> logout(Long memberId) {
        String key = REFRESH_TOKEN_PREFIX + memberId;
        return reactiveRedisTemplate.delete(key)
            .doOnSuccess(count -> log.info("로그아웃 완료: memberId={}", memberId))
            .then();
    }

    /**
     * 토큰 검증
     */
    public Mono<Boolean> validateToken(String token) {
        return jwtTokenProvider.validateTokenReactive(token);
    }

    /**
     * 토큰에서 회원 ID 추출
     */
    public Mono<Long> extractMemberId(String token) {
        return jwtTokenProvider.getMemberIdReactive(token);
    }

    // ========== Private Methods (함수형 헬퍼) ==========

    /**
     * 비밀번호 검증 (Blocking 작업을 별도 스레드에서)
     */
    private Mono<Boolean> validatePassword(String rawPassword, String encodedPassword) {
        return Mono.fromCallable(() -> passwordEncoder.matches(rawPassword, encodedPassword))
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 회원 상태 검증
     */
    private Mono<Member> validateMemberStatus(Member member) {
        return switch (member.getStatus()) {
            case ACTIVE -> Mono.just(member);
            case DORMANT -> Mono.error(new BusinessException(ErrorCode.DORMANT_MEMBER));
            case WITHDRAWN -> Mono.error(new BusinessException(ErrorCode.WITHDRAWN_MEMBER));
            case SUSPENDED -> Mono.error(new BusinessException(ErrorCode.SUSPENDED_MEMBER));
        };
    }

    /**
     * 마지막 로그인 시간 업데이트
     */
    private Mono<Member> updateLastLogin(Member member) {
        return Mono.just(member.login())
            .flatMap(memberRepository::save);
    }

    /**
     * 토큰 생성 및 저장
     */
    private Mono<TokenResponse> generateTokens(Member member) {
        return Mono.fromCallable(() -> {
                String accessToken = jwtTokenProvider.createAccessToken(member.getId());
                String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());
                return TokenResponse.of(accessToken, refreshToken);
            })
            .flatMap(tokenResponse ->
                saveRefreshToken(member.getId(), tokenResponse.getRefreshToken())
                    .thenReturn(tokenResponse)
            );
    }

    /**
     * Access Token만 생성 (갱신 시)
     */
    private Mono<TokenResponse> generateAccessToken(Long memberId) {
        return Mono.fromCallable(() -> {
            String accessToken = jwtTokenProvider.createAccessToken(memberId);
            // 기존 Refresh Token은 유지
            return TokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();
        });
    }

    /**
     * Refresh Token Redis에 저장
     */
    private Mono<Boolean> saveRefreshToken(Long memberId, String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + memberId;
        return reactiveRedisTemplate.opsForValue()
            .set(key, refreshToken, REFRESH_TOKEN_TTL);
    }

    /**
     * Refresh Token 검증 (Redis 확인)
     */
    private Mono<Long> verifyRefreshToken(Long memberId) {
        String key = REFRESH_TOKEN_PREFIX + memberId;
        return reactiveRedisTemplate.hasKey(key)
            .flatMap(exists -> exists
                ? Mono.just(memberId)
                : Mono.error(new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND)));
    }

    /**
     * 인증 예외 매핑
     */
    private Throwable mapAuthException(Throwable error) {
        if (error instanceof BusinessException) {
            return error;
        }

        log.error("인증 처리 중 오류 발생", error);
        return new BusinessException(ErrorCode.AUTHENTICATION_FAILED);
    }
}
