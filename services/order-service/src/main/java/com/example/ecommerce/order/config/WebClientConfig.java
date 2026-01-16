package com.example.ecommerce.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${services.member-service.url}")
    private String memberServiceUrl;

    @Value("${services.product-service.url}")
    private String productServiceUrl;

    @Bean
    public WebClient memberServiceClient(WebClient.Builder builder) {
        return builder
                .baseUrl(memberServiceUrl)
                .build();
    }

    @Bean
    public WebClient productServiceClient(WebClient.Builder builder) {
        return builder
                .baseUrl(productServiceUrl)
                .build();
    }
}
