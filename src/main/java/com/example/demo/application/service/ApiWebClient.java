package com.example.demo.application.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiWebClient {

    private final WebClient webClient;

    public <T> Mono<T> get(String url,
            Class<T> clazz,
            Map<String, String> headers,
            CircuitBreaker cb,
            Retry retry) {

        WebClient.RequestHeadersSpec<?> req = webClient.get().uri(url);

        if (headers != null)
            req = req.headers(h -> headers.forEach(h::add));

        Mono<T> mono = req.retrieve()
                .onStatus(s -> s.value() == 500 || s.value() == 503 || s.value() == 429,
                        r -> r.bodyToMono(String.class)
                                .map(RuntimeException::new))
                .bodyToMono(clazz);

        if (retry != null)
            mono = mono.transformDeferred(RetryOperator.of(retry));

        if (cb != null)
            mono = mono.transformDeferred(CircuitBreakerOperator.of(cb));

        return mono;
    }

    public <T, R> Mono<T> post(String url, R body,
            Class<T> clazz,
            Map<String, String> headers,
            CircuitBreaker cb,
            Retry retry) {

        WebClient.RequestBodySpec req = webClient.post().uri(url);

        if (headers != null)
            req = req.headers(h -> headers.forEach(h::add));

        Mono<T> mono = req.bodyValue(body)
                .retrieve()
                .onStatus(s -> s.value() == 500 || s.value() == 503 || s.value() == 429,
                        r -> r.bodyToMono(String.class)
                                .map(RuntimeException::new))
                .bodyToMono(clazz);

        if (retry != null)
            mono = mono.transformDeferred(RetryOperator.of(retry));

        if (cb != null)
            mono = mono.transformDeferred(CircuitBreakerOperator.of(cb));

        return mono;
    }
}