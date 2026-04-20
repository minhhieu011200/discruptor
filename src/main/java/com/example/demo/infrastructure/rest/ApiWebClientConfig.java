package com.example.demo.infrastructure.rest;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import io.netty.channel.ChannelOption;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import jakarta.annotation.PostConstruct;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ApiWebClientConfig {
        @Bean
        public WebClient webClient(WebClient.Builder builder) {

                HttpClient httpClient = HttpClient.create()
                                .keepAlive(true)
                                .compress(true)
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                                .responseTimeout(Duration.ofSeconds(30))
                                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(5))
                                                .addHandlerLast(new WriteTimeoutHandler(5)));

                return builder
                                .clientConnector(new ReactorClientHttpConnector(httpClient))
                                .filter(logAll())
                                .build();
        }

        private ExchangeFilterFunction logAll() {
                return (request, next) -> {

                        long startTime = System.nanoTime();
                        String traceId = getOrCreateTraceId();

                        ClientRequest newRequest = ClientRequest.from(request)
                                        .header("X-Trace-Id", traceId)
                                        .attribute("startTime", startTime)
                                        .build();

                        log.info("➡️ REQUEST traceId={} method={} url={}",
                                        traceId,
                                        request.method(),
                                        request.url());

                        Mono<ClientResponse> mono = next.exchange(newRequest)
                                        .doOnNext(response -> {
                                                long duration = (System.nanoTime() - startTime) / 1_000_000;
                                                log.info("⬅️ RESPONSE traceId={} status={} latency={}ms",
                                                                traceId,
                                                                response.statusCode().value(),
                                                                duration);
                                        })
                                        .doOnError(ex -> {
                                                long duration = (System.nanoTime() - startTime) / 1_000_000;
                                                log.error("❌ ERROR traceId={} latency={}ms error={}",
                                                                traceId,
                                                                duration,
                                                                ex.toString());
                                        });

                        return mono;
                };
        }

        // =========================
        // TRACE ID
        // =========================
        private String getOrCreateTraceId() {
                String traceId = MDC.get("traceId");
                if (traceId == null) {
                        traceId = UUID.randomUUID().toString();
                        MDC.put("traceId", traceId);
                }
                return traceId;
        }

        // truncate body tránh log quá lớn
        private String truncate(String body) {
                if (body == null)
                        return "";
                return body.length() > 500 ? body.substring(0, 500) + "..." : body;
        }

        @PostConstruct
        public void init() {
                reactor.core.publisher.Hooks.enableAutomaticContextPropagation();
        }

}