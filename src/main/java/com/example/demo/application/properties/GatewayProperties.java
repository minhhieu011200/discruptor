package com.example.demo.application.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "gateway")
@Data
public class GatewayProperties {
    private String baseUrl;
    private String cbName;
    private String retryName;
}