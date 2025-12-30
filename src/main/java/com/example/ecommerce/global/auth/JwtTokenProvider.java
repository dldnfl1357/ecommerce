package com.example.ecommerce.global.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:defaultSecretKeyForDevelopmentOnlyMinimum32Characters!!}")
    private String secret;

    @Value("${jwt.access-token-expiration:3600000}")  // 1시간
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800000}")  // 7일
    private long refreshTokenExpiration;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        log.info("JWT SecretKey initialized");
    }

    /**
     * Access Token 생성
     */
    public String createAccessToken(Long memberId) {
        return createToken(memberId, accessTokenExpiration);
    }

    /**
     * Refresh Token 생성
     */
    public String createRefreshToken(Long memberId) {
        return createToken(memberId, refreshTokenExpiration);
    }

    /**
     * 토큰 생성 (공통 로직)
     */
    private String createToken(Long memberId, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(memberId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 토큰에서 회원 ID 추출
     */
    public Long getMemberId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.parseLong(claims.getSubject());
    }

    /**
     * 토큰 검증
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    // ========== Reactive Methods ==========

    /**
     * 토큰 검증 (Reactive)
     */
    public Mono<Boolean> validateTokenReactive(String token) {
        return Mono.fromCallable(() -> validateToken(token))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorReturn(false);
    }

    /**
     * 토큰에서 회원 ID 추출 (Reactive)
     */
    public Mono<Long> getMemberIdReactive(String token) {
        return Mono.fromCallable(() -> getMemberId(token))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    log.error("Failed to extract member ID from token", e);
                    return Mono.empty();
                });
    }
}
