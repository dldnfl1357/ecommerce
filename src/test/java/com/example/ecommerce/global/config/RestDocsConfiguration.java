package com.example.ecommerce.global.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentationConfigurer;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;

@TestConfiguration
public class RestDocsConfiguration {

    @Bean
    public WebTestClientRestDocumentationConfigurer restDocumentationConfigurer() {
        return new WebTestClientRestDocumentationConfigurer()
            .operationPreprocessors()
            .withRequestDefaults(
                modifyUris()
                    .scheme("https")
                    .host("api.coupang.example.com")
                    .removePort(),
                prettyPrint()
            )
            .withResponseDefaults(
                prettyPrint()
            );
    }
}
