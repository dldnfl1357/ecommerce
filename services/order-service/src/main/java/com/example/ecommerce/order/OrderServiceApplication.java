package com.example.ecommerce.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.example.ecommerce.order",
        "com.example.ecommerce.common"
})
@EnableR2dbcRepositories(basePackages = "com.example.ecommerce.order")
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
