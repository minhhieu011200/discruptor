package com.example.demo.application.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import com.example.demo.application.dto.SubscribeExchangeRequestDTO;
import com.example.demo.domain.service.GatewayApiService;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import com.example.demo.application.properties.GatewayProperties;

@Service
public class GatewayApiImplement implements GatewayApiService {

    private final ApiWebClient apiWebClient;
    private final CircuitBreaker cb;
    private final Retry retry;
    private final GatewayProperties properties;

    public GatewayApiImplement(ApiWebClient apiWebClient,
            CircuitBreakerRegistry cbRegistry,
            RetryRegistry retryRegistry,
            GatewayProperties properties) {

        this.apiWebClient = apiWebClient;
        this.properties = properties;

        // Tạo 1 lần dùng chung
        this.cb = cbRegistry.circuitBreaker(properties.getCbName());
        this.retry = retryRegistry.retry(properties.getRetryName());
    }

    private Map<String, String> jsonHeaders() {
        return Map.of("Content-Type", "application/json");
    }

    /** Generic method tránh trùng code */
    private <T> void callApi(String path, Object body, Class<T> responseType) {
        apiWebClient.post(
                properties.getBaseUrl() + path,
                body,
                responseType,
                jsonHeaders(),
                cb,
                retry).subscribe();
    }

    @Override
    public void subscribe(SubscribeExchangeRequestDTO data) {
        callApi("/subscription/subscribe", data, String.class);
    }

    @Override
    public void unsubscribe(SubscribeExchangeRequestDTO data) {
        callApi("/subscription/unsubscribe", data, String.class);
    }
}