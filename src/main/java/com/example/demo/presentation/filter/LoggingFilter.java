package com.example.demo.presentation.filter;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // Chạy trước Spring Security (order = -100)
public class LoggingFilter implements WebFilter {

    private static final DataBufferFactory BUFFER_FACTORY = new DefaultDataBufferFactory();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod().name();
        String uri = request.getURI().getPath();
        long startTime = System.currentTimeMillis();

        // Wrap request để đọc body mà không mất stream
        ServerHttpRequestDecorator requestDecorator = new ServerHttpRequestDecorator(request) {
            @Override
            public Flux<DataBuffer> getBody() {
                return DataBufferUtils.join(super.getBody())
                        .doOnNext(dataBuffer -> {
                            String requestBody = dataBuffer.toString(StandardCharsets.UTF_8);
                            log.info("[REQ] {} {} | body={}", method, uri, requestBody);
                            DataBufferUtils.retain(dataBuffer);
                        })
                        .flatMapMany(dataBuffer -> {
                            DataBuffer copy = BUFFER_FACTORY.wrap(dataBuffer.asByteBuffer().array());
                            DataBufferUtils.release(dataBuffer);
                            return Flux.just(copy);
                        });
            }
        };

        // Wrap response để đọc body trước khi ghi ra client
        ServerHttpResponseDecorator responseDecorator = new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                Flux<DataBuffer> bufferedBody = Flux.from(body)
                        .doOnNext(dataBuffer -> {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.toByteBuffer().get(bytes);
                            String responseBody = new String(bytes, StandardCharsets.UTF_8);
                            long duration = System.currentTimeMillis() - startTime;
                            log.info("[RES] {} {} | status={} | body={} | duration={}ms",
                                    method, uri,
                                    getStatusCode(),
                                    responseBody,
                                    duration);
                        });
                return super.writeWith(bufferedBody);
            }
        };

        ServerWebExchange decoratedExchange = exchange.mutate()
                .request(requestDecorator)
                .response(responseDecorator)
                .build();

        // Log request khi không có body (GET, DELETE, ...)
        if (!hasBody(request)) {
            log.info("[REQ] {} {}", method, uri);
        }

        return chain.filter(decoratedExchange);
    }

    private boolean hasBody(ServerHttpRequest request) {
        String method = request.getMethod().name();
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }
}
