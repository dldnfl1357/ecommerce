package com.example.ecommerce.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Actuator
                        .pathMatchers("/actuator/**").permitAll()
                        // Swagger
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**").permitAll()
                        // Public API - 상품 조회
                        .pathMatchers("/api/v1/products/**", "/api/v1/categories/**").permitAll()
                        // Internal API (서비스 간 통신)
                        .pathMatchers("/internal/**").permitAll()
                        // 나머지는 인증 필요
                        .anyExchange().authenticated()
                )
                .build();
    }
}
