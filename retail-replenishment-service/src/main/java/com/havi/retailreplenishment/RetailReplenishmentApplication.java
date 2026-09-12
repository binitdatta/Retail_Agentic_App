package com.havi.retailreplenishment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RetailReplenishmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(RetailReplenishmentApplication.class, args);
    }
}
