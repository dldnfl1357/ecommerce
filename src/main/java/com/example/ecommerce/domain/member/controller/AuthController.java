package com.example.ecommerce.domain.member.controller;

import com.example.ecommerce.domain.member.dto.request.LoginRequest;
import com.example.ecommerce.domain.member.dto.request.SignupRequest;
import com.example.ecommerce.domain.member.dto.response.MemberResponse;
import com.example.ecommerce.domain.member.dto.response.TokenResponse;
import com.example.ecommerce.domain.member.service.AuthService;
import com.example.ecommerce.domain.member.service.MemberService;
import com.example.ecommerce.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * 인증 관련 API Controller (WebFlux)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;
    private final AuthService authService;

    /**
     * 회원 가입
     * POST /api/v1/auth/signup
     */
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<MemberResponse>> signup(
        @Valid @RequestBody SignupRequest request
    ) {
        log.info("회원 가입 요청: email={}", request.getEmail());

        return memberService.signup(request)
            .map(response -> ApiResponse.success(response, "회원 가입이 완료되었습니다."))
            .doOnSuccess(response -> log.info("회원 가입 성공: {}", request.getEmail()))
            .doOnError(error -> log.error("회원 가입 실패: email={}", request.getEmail(), error));
    }

    /**
     * 로그인
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public Mono<ApiResponse<TokenResponse>> login(
        @Valid @RequestBody LoginRequest request
    ) {
        log.info("로그인 요청: email={}", request.getEmail());

        return authService.login(request)
            .map(tokenResponse -> ApiResponse.success(tokenResponse, "로그인에 성공했습니다."))
            .doOnSuccess(response -> log.info("로그인 성공: {}", request.getEmail()))
            .doOnError(error -> log.error("로그인 실패: email={}", request.getEmail(), error));
    }

    /**
     * 토큰 갱신
     * POST /api/v1/auth/refresh
     */
    @PostMapping("/refresh")
    public Mono<ApiResponse<TokenResponse>> refresh(
        @RequestHeader("Refresh-Token") String refreshToken
    ) {
        log.info("토큰 갱신 요청");

        return authService.refresh(refreshToken)
            .map(tokenResponse -> ApiResponse.success(tokenResponse, "토큰이 갱신되었습니다."))
            .doOnSuccess(response -> log.info("토큰 갱신 성공"))
            .doOnError(error -> log.error("토큰 갱신 실패", error));
    }

    /**
     * 로그아웃
     * POST /api/v1/auth/logout
     */
    @PostMapping("/logout")
    public Mono<ApiResponse<Void>> logout(
        @RequestAttribute("memberId") Long memberId
    ) {
        log.info("로그아웃 요청: memberId={}", memberId);

        return authService.logout(memberId)
            .then(Mono.just(ApiResponse.success("로그아웃되었습니다.")))
            .doOnSuccess(response -> log.info("로그아웃 성공: memberId={}", memberId))
            .doOnError(error -> log.error("로그아웃 실패: memberId={}", memberId, error));
    }

    /**
     * 토큰 검증
     * GET /api/v1/auth/validate
     */
    @GetMapping("/validate")
    public Mono<ApiResponse<Boolean>> validateToken(
        @RequestHeader("Authorization") String authorization
    ) {
        String token = extractToken(authorization);

        return authService.validateToken(token)
            .map(isValid -> ApiResponse.success(isValid, "토큰 검증 완료"))
            .defaultIfEmpty(ApiResponse.success(false, "유효하지 않은 토큰"));
    }

    // Helper method
    private String extractToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return authorization;
    }
}
