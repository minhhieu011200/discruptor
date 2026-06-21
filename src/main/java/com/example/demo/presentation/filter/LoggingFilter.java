package com.example.demo.presentation.filter;

import com.example.demo.infrastructure.config.MDCPropagationHook;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Logging filter – chạy sau {@link BodyCachingFilter} (order =
 * HIGHEST_PRECEDENCE + 5).
 *
 * <p>
 * Body request đã được {@link BodyCachingFilter} đọc và cache vào
 * {@code exchange attribute}, filter này chỉ cần lấy ra mà không cần
 * join stream lần thứ hai → không tốn thêm bộ nhớ.
 * </p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5) // Sau BodyCachingFilter (HIGHEST_PRECEDENCE)
public class LoggingFilter implements WebFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod().name();
        String uri = request.getURI().getPath();
        String queryStr = request.getURI().getRawQuery();
        if (queryStr == null || queryStr.isEmpty()) {
            queryStr = "-";
        }
        long startTime = System.currentTimeMillis();

        // Lấy traceId từ header inbound hoặc tự sinh UUID
        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        final String tid = traceId;

        // Gắn traceId vào response header để client/downstream biết
        exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, tid);

        // ── Lấy body từ cache (đã đọc bởi BodyCachingFilter) ──────────────
        byte[] cachedBody = exchange.getAttribute(BodyCachingFilter.CACHED_BODY_ATTR);
        String bodyStr = (cachedBody != null && cachedBody.length > 0)
                ? new String(cachedBody, StandardCharsets.UTF_8)
                : "";

        // Log request ngay lập tức (không cần wrap request)
        if (hasBody(request)) {
            log.info("[REQ] [{}] {} {} | query={} | body={}", tid, method, uri, queryStr, bodyStr);
        } else {
            log.info("[REQ] [{}] {} {} | query={}", tid, method, uri, queryStr);
        }

        // ── MDC map ────────────────────────────────────────────────────────
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("traceId", tid);

        // ── Wrap response để log body + duration ───────────────────────────
        ServerHttpResponseDecorator responseDecorator = new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                Flux<DataBuffer> bufferedBody = Flux.from(body)
                        .cast(DataBuffer.class)
                        .doOnNext(dataBuffer -> {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.toByteBuffer().get(bytes);
                            String responseBody = new String(bytes, StandardCharsets.UTF_8);
                            long duration = System.currentTimeMillis() - startTime;
                            String ec = "-";
                            String em = "-";
                            MediaType contentType = exchange.getResponse().getHeaders().getContentType();
                            boolean isJson = contentType != null && contentType.includes(MediaType.APPLICATION_JSON);
                            if (isJson && responseBody != null && responseBody.startsWith("{")
                                    && (responseBody.contains("\"ec\"") || responseBody.contains("\"em\""))) {
                                try {
                                    JsonNode node = objectMapper.readTree(responseBody);
                                    if (node.has("ec")) {
                                        ec = node.get("ec").asText();
                                    }
                                    if (node.has("em")) {
                                        em = node.get("em").asText();
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                            log.info("[RES] [{}] {} {} | status={} | ec={} | em={} | duration={}ms",
                                    tid, method, uri,
                                    getStatusCode(),
                                    ec,
                                    em,
                                    duration);
                        });
                return super.writeWith(bufferedBody);
            }
        };

        ServerWebExchange decoratedExchange = exchange.mutate()
                .response(responseDecorator)
                .build();

        // Đưa mdcMap vào Reactor Context → MDCPropagationHook tự copy vào MDC
        return chain.filter(decoratedExchange)
                .contextWrite(Context.of(MDCPropagationHook.MDC_CONTEXT_KEY, mdcMap));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean hasBody(ServerHttpRequest request) {
        String m = request.getMethod().name();
        return "POST".equals(m) || "PUT".equals(m) || "PATCH".equals(m);
    }

}
