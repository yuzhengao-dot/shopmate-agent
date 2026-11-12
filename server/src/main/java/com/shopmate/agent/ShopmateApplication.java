package com.shopmate.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ShopmateApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopmateApplication.class, args);
    }
}