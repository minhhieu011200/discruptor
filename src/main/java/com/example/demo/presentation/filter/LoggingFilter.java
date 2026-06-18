package com.example.demo.presentation.filter;

import com.example.demo.infrastructure.config.MDCPropagationHook;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // Chạy trước Spring Security (order = -100)
public class LoggingFilter implements WebFilter {

    private static final DataBufferFactory BUFFER_FACTORY  = new DefaultDataBufferFactory();
    private static final String            TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String method    = request.getMethod().name();
        String uri       = request.getURI().getPath();
        String queryStr  = buildQueryString(request.getQueryParams());
        long   startTime = System.currentTimeMillis();

        // Lấy traceId từ header inbound hoặc tự sinh UUID
        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        final String tid = traceId;

        // Gắn traceId vào response header để client/downstream biết
        exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, tid);

        // ── MDC map: thêm bất kỳ field nào cần propagate ─────────────────────
        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("traceId", tid);
        // mdcMap.put("userId", ...);      ← thêm sau khi có auth
        // mdcMap.put("accountId", ...);   ← thêm sau khi parse token

        // Wrap request để đọc body mà không mất stream
        ServerHttpRequestDecorator requestDecorator = new ServerHttpRequestDecorator(request) {
            @Override
            public Flux<DataBuffer> getBody() {
                return DataBufferUtils.join(super.getBody())
                        .doOnNext(dataBuffer -> {
                            String body = dataBuffer.toString(StandardCharsets.UTF_8);
                            log.info("[REQ] [{}] {} {} | query={} | body={}", tid, method, uri, queryStr, body);
                            DataBufferUtils.retain(dataBuffer);
                        })
                        .flatMapMany(dataBuffer -> {
                            DataBuffer copy = BUFFER_FACTORY.wrap(dataBuffer.asByteBuffer().array());
                            DataBufferUtils.release(dataBuffer);
                            return Flux.just(copy);
                        });
            }
        };

        // Wrap response để log body + duration trước khi ghi ra client
        ServerHttpResponseDecorator responseDecorator = new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                Flux<DataBuffer> bufferedBody = Flux.from(body)
                        .doOnNext(dataBuffer -> {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.toByteBuffer().get(bytes);
                            String responseBody = new String(bytes, StandardCharsets.UTF_8);
                            long duration = System.currentTimeMillis() - startTime;
                            log.info("[RES] [{}] {} {} | status={} | body={} | duration={}ms",
                                    tid, method, uri,
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

        // GET / DELETE / ... không có body → log ngay
        if (!hasBody(request)) {
            log.info("[REQ] [{}] {} {} | query={}", tid, method, uri, queryStr);
        }

        // Đưa mdcMap vào Reactor Context → MDCPropagationHook tự copy vào MDC
        return chain.filter(decoratedExchange)
                .contextWrite(Context.of(MDCPropagationHook.MDC_CONTEXT_KEY, mdcMap));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean hasBody(ServerHttpRequest request) {
        String m = request.getMethod().name();
        return "POST".equals(m) || "PUT".equals(m) || "PATCH".equals(m);
    }

    private String buildQueryString(MultiValueMap<String, String> params) {
        if (params == null || params.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        params.forEach((key, values) -> {
            for (String val : values) {
                if (sb.length() > 0) sb.append('&');
                sb.append(key).append('=').append(val);
            }
        });
        return sb.toString();
    }
}
