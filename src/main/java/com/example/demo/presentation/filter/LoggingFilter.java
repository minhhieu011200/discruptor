package com.example.demo.presentation.filter;

import com.example.demo.infrastructure.config.MDCPropagationHook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

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

    @Value("${app.logging.request-response.enabled:true}")
    private boolean loggingEnabled;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!loggingEnabled) {
            return chain.filter(exchange);
        }
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod().name();
        String uri = request.getURI().getPath();
        String queryStr = request.getURI().getRawQuery();
        if (queryStr == null || queryStr.isEmpty()) {
            queryStr = "-";
        }
        long startTime = System.currentTimeMillis();

        // Lấy traceId từ header inbound hoặc tự sinh traceId lock-free
        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            long traceIdVal = ThreadLocalRandom.current().nextLong();
            traceId = Long.toHexString(traceIdVal);
        }
        final String tid = traceId;

        // Gắn traceId vào response header để client/downstream biết
        exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, tid);

        // ── Lấy body từ cache (đã đọc bởi BodyCachingFilter) ──────────────
        byte[] cachedBody = exchange.getAttribute(BodyCachingFilter.CACHED_BODY_ATTR);
        String bodyStr = (cachedBody != null && cachedBody.length > 0)
                ? new String(cachedBody, StandardCharsets.UTF_8)
                : "";

        log.info("[REQ] [{}] {} {} | query={} | body={}", tid, method, uri, queryStr, bodyStr);

        // ── MDC map ────────────────────────────────────────────────────────
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("traceId", tid);

        // Đưa mdcMap vào Reactor Context → MDCPropagationHook tự copy vào MDC
        return chain.filter(exchange)
                .doOnSuccess(aVoid -> {
                    long duration = System.currentTimeMillis() - startTime;
                    String ec = exchange.getResponse().getHeaders().getFirst("X-Error-Code");
                    String em = exchange.getResponse().getHeaders().getFirst("X-Error-Message");
                    if (ec == null)
                        ec = "0";
                    if (em == null)
                        em = "Success";
                    log.info("[RES] [{}] {} {} | status={} | ec={} | em={} | duration={}ms",
                            tid, method, uri,
                            exchange.getResponse().getStatusCode(),
                            ec,
                            em,
                            duration);
                })
                .doOnError(throwable -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("[RES] [{}] {} {} | status={} | ec=500 | em={} | duration={}ms",
                            tid, method, uri,
                            exchange.getResponse().getStatusCode(),
                            throwable.getMessage(),
                            duration);
                })
                .contextWrite(Context.of(MDCPropagationHook.MDC_CONTEXT_KEY, mdcMap));
    }
}
