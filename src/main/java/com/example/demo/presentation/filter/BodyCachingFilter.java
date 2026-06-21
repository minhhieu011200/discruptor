package com.example.demo.presentation.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Filter chạy ĐẦU TIÊN trong pipeline, đọc request body đúng một lần
 * rồi lưu vào {@code exchange attribute} {@link #CACHED_BODY_ATTR}.
 *
 * <p>Các filter downstream (LoggingFilter, HmacAuthFilter) chỉ cần gọi
 * {@code exchange.getAttribute(BodyCachingFilter.CACHED_BODY_ATTR)} để lấy
 * {@code byte[]} mà không cần join body lần thứ hai.</p>
 *
 * <p>Request được wrap bằng {@link CachedBodyRequest} để các handler
 * phía sau vẫn gọi {@code getBody()} bình thường.</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BodyCachingFilter implements WebFilter {

    /** Key lưu cached body (byte[]) vào ServerWebExchange attributes. */
    public static final String CACHED_BODY_ATTR = "CACHED_BODY_BYTES";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return DataBufferUtils.join(exchange.getRequest().getBody())
                .defaultIfEmpty(exchange.getResponse().bufferFactory().wrap(new byte[0]))
                .flatMap(dataBuffer -> {
                    // Đọc bytes một lần duy nhất
                    byte[] bodyBytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.toByteBuffer().get(bodyBytes);
                    DataBufferUtils.release(dataBuffer);

                    // Lưu vào attribute để LoggingFilter và HmacAuthFilter dùng lại
                    exchange.getAttributes().put(CACHED_BODY_ATTR, bodyBytes);

                    // Wrap request để downstream handler vẫn đọc được body qua getBody()
                    ServerHttpRequest wrappedRequest =
                            new CachedBodyRequest(exchange.getRequest(), bodyBytes, exchange);

                    return chain.filter(exchange.mutate().request(wrappedRequest).build());
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inner class: cung cấp lại body đã cache cho mọi handler downstream
    // ─────────────────────────────────────────────────────────────────────────

    private static class CachedBodyRequest extends ServerHttpRequestDecorator {

        private final byte[]           cachedBody;
        private final ServerWebExchange exchange;

        CachedBodyRequest(ServerHttpRequest delegate, byte[] cachedBody, ServerWebExchange exchange) {
            super(delegate);
            this.cachedBody = cachedBody;
            this.exchange   = exchange;
        }

        @Override
        public Flux<DataBuffer> getBody() {
            return Flux.defer(() -> {
                if (cachedBody == null || cachedBody.length == 0) {
                    return Flux.empty();
                }
                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(cachedBody);
                return Flux.just(buffer);
            });
        }
    }
}
