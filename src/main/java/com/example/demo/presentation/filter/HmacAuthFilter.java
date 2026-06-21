package com.example.demo.presentation.filter;

import com.example.demo.application.properties.S2sSecurityProperties;
import com.example.demo.infrastructure.security.HmacAuthenticationToken;
import com.example.demo.infrastructure.security.HmacSignatureUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * WebFilter xác thực Server-to-Server bằng HMAC-SHA256.
 *
 * <h3>Headers yêu cầu từ phía client gọi đến:</h3>
 * <ul>
 * <li>{@code X-Api-Key-Id} – ID của service gọi (phải có trong config)</li>
 * <li>{@code X-Timestamp} – Unix epoch milliseconds (String)</li>
 * <li>{@code X-Signature} – Base64( HMAC-SHA256(secret, payload) )</li>
 * </ul>
 *
 * <h3>Payload để ký:</h3>
 *
 * <pre>
 *   HTTP_METHOD\nREQUEST_PATH\nTIMESTAMP_MS\nHEX_SHA256(body)
 * </pre>
 *
 * <h3>Bật / tắt:</h3>
 *
 * <pre>
 *   security.s2s.enabled=true   # false → bỏ qua filter hoàn toàn
 * </pre>
 *
 * <p>
 * Order = HIGHEST_PRECEDENCE + 10 → chạy sau {@link BodyCachingFilter} và
 * {@link LoggingFilter}, body đã được cache sẵn trong exchange attribute
 * {@link BodyCachingFilter#CACHED_BODY_ATTR} – không cần join stream lần 2.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class HmacAuthFilter implements WebFilter {

    private final S2sSecurityProperties props;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        // 1. Nếu tắt xác thực → pass-through
        if (!props.isEnabled()) {
            log.debug("[S2S] Authentication disabled – skip filter");
            return chain.filter(exchange);
        }

        // 2. Permit-paths → bỏ qua
        String path = exchange.getRequest().getURI().getPath();
        if (isPermitted(path)) {
            log.debug("[S2S] Permitted path: {} – skip filter", path);
            return chain.filter(exchange);
        }

        // 3. Lấy body từ cache (đã đọc 1 lần bởi BodyCachingFilter)
        byte[] bodyBytes = exchange.getAttribute(BodyCachingFilter.CACHED_BODY_ATTR);
        if (bodyBytes == null)
            bodyBytes = new byte[0];

        return authenticate(exchange, chain, bodyBytes);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core authentication logic
    // ─────────────────────────────────────────────────────────────────────────

    private Mono<Void> authenticate(ServerWebExchange exchange,
            WebFilterChain chain,
            byte[] bodyBytes) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();
        // Lấy query string (không có dấu '?') để đưa vào payload ký
        // Bảo vệ GET request khỏi bị tamper query params
        String queryString = request.getURI().getRawQuery(); // null nếu không có
        if (queryString == null)
            queryString = "";

        // Lấy headers bắt buộc
        String keyId = request.getHeaders().getFirst(HmacSignatureUtil.HEADER_KEY_ID);
        String timestamp = request.getHeaders().getFirst(HmacSignatureUtil.HEADER_TIMESTAMP);
        String signature = request.getHeaders().getFirst(HmacSignatureUtil.HEADER_SIGNATURE);

        // Validate headers tồn tại
        if (isBlank(keyId) || isBlank(timestamp) || isBlank(signature)) {
            log.warn("[S2S] Missing required headers on {} {} | keyId={} ts={} sig={}",
                    method, path, keyId, timestamp, signature);
            return rejectUnauthorized(exchange, "Missing required S2S authentication headers");
        }

        // Validate timestamp window
        if (!isTimestampValid(timestamp)) {
            log.warn("[S2S] Timestamp expired or invalid: {} on {} {}", timestamp, method, path);
            return rejectUnauthorized(exchange, "Request timestamp is outside allowed window");
        }

        // Lookup secret key
        Map<String, String> keys = props.getKeys();
        String secretKey = keys.get(keyId);
        if (secretKey == null) {
            log.warn("[S2S] Unknown key-id: {} on {} {}", keyId, method, path);
            return rejectUnauthorized(exchange, "Unknown API key id");
        }

        // Tính lại HMAC và so sánh (constant-time)
        // queryString được đưa vào payload để GET params không bị tamper
        try {
            final String qs = queryString;
            String expected = HmacSignatureUtil.computeSignature(secretKey, method, path, qs, timestamp, bodyBytes);
            if (!HmacSignatureUtil.verifySignature(expected, signature)) {
                log.warn("[S2S] Invalid signature for key-id={} on {} {}", keyId, method, path);
                return rejectUnauthorized(exchange, "Invalid HMAC signature");
            }
        } catch (Exception e) {
            log.error("[S2S] Error computing HMAC for {} {}: {}", method, path, e.getMessage(), e);
            return rejectUnauthorized(exchange, "Authentication processing error");
        }

        // Xác thực thành công – inject SecurityContext, không cần wrap request lại
        // vì BodyCachingFilter đã wrap từ đầu pipeline
        log.debug("[S2S] Authenticated key-id={} on {} {}", keyId, method, path);
        HmacAuthenticationToken authToken = new HmacAuthenticationToken(keyId);

        return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authToken));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isPermitted(String path) {
        List<String> permitPaths = props.getPermitPaths();
        if (permitPaths == null || permitPaths.isEmpty())
            return false;
        return permitPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private boolean isTimestampValid(String timestampStr) {
        try {
            long requestTs = Long.parseLong(timestampStr);
            long nowMs = System.currentTimeMillis();
            long skewMs = props.getClockSkewSeconds() * 1000L;
            return Math.abs(nowMs - requestTs) <= skewMs;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private Mono<Void> rejectUnauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body;
        try {
            body = objectMapper.writeValueAsString(
                    Map.of("error", "Unauthorized", "message", message));
        } catch (Exception e) {
            body = "{\"error\":\"Unauthorized\"}";
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
