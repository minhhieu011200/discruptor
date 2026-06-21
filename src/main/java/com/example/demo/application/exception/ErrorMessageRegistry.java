package com.example.demo.application.exception;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Scan toàn bộ key pattern "error:*" từ Redis và cache vào bộ nhớ.
 *
 * Redis key format: error:{ec}
 * Ví dụ:
 * SET error:1001 "Mã chứng khoán không tồn tại"
 * SET error:1002 "Tài khoản không hợp lệ"
 * SET error:1003 "Dữ liệu đầu vào không hợp lệ"
 *
 * Không cần khai báo trước trong enum — thêm key mới trong Redis là tự động có.
 * Fallback: nếu code không có trong cache → dùng message mặc định trong
 * ErrorCode enum.
 */
@Slf4j
@Component
public class ErrorMessageRegistry {

    private static final String REDIS_KEY_PREFIX = "error:";
    private static final String SCAN_PATTERN = REDIS_KEY_PREFIX + "*";

    /** Null khi Redis bị tắt (app.connection.redis.enabled=false). */
    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    /**
     * In-memory cache: code string → message
     * Ví dụ: "1001" → "Mã chứng khoán không tồn tại"
     */
    private final Map<String, String> messageCache = new ConcurrentHashMap<>();

    // ── Load ───────────────────────────────────────────────────────────────────

    @PostConstruct
    public void loadAll() {
        if (redisTemplate == null) {
            log.warn("[ErrorMessageRegistry] Redis is disabled – using default messages from ErrorCode enum");
            return;
        }
        Map<String, String> loaded = scanFromRedis();
        messageCache.clear();
        messageCache.putAll(loaded);
        log.info("[ErrorMessageRegistry] Loaded {} error messages from Redis (pattern={})",
                messageCache.size(), SCAN_PATTERN);
        if (log.isDebugEnabled()) {
            messageCache.forEach((k, v) -> log.debug("  error:{} = {}", k, v));
        }
    }

    // ── Get ────────────────────────────────────────────────────────────────────

    /**
     * Lấy message theo ErrorCode enum.
     * Ưu tiên: Redis cache → default message trong enum.
     */
    public String getMessage(ErrorCode errorCode) {
        return messageCache.getOrDefault(
                String.valueOf(errorCode.getCode()),
                errorCode.getMessage());
    }

    /**
     * Lấy message theo mã lỗi integer (không cần enum).
     * Ưu tiên: Redis cache → null nếu không tìm thấy.
     */
    public String getMessage(int code) {
        return messageCache.get(String.valueOf(code));
    }

    // ── Refresh ────────────────────────────────────────────────────────────────

    /** Reload toàn bộ từ Redis (dùng cho admin endpoint hoặc schedule). */
    public void refreshAll() {
        if (redisTemplate == null)
            return;
        Map<String, String> latest = scanFromRedis();
        messageCache.clear();
        messageCache.putAll(latest);
        log.info("[ErrorMessageRegistry] Refreshed {} error messages from Redis", messageCache.size());
    }

    /** Reload 1 error code cụ thể. */
    public void refresh(ErrorCode errorCode) {
        if (redisTemplate == null)
            return;
        String codeStr = String.valueOf(errorCode.getCode());
        String key = REDIS_KEY_PREFIX + codeStr;
        String value = redisTemplate.opsForValue().get(key);
        if (value != null && !value.isBlank()) {
            messageCache.put(codeStr, value);
            log.info("[ErrorMessageRegistry] Refreshed {}: {}", codeStr, value);
        } else {
            messageCache.remove(codeStr);
            log.info("[ErrorMessageRegistry] Removed {} from cache, fallback to default", codeStr);
        }
    }

    // ── Internal ───────────────────────────────────────────────────────────────

    /**
     * SCAN thay vì KEYS để an toàn trên production Redis.
     * Trả về map: codeString → message.
     */
    private Map<String, String> scanFromRedis() {
        Map<String, String> result = new ConcurrentHashMap<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(SCAN_PATTERN)
                .count(100)
                .build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String fullKey = cursor.next(); // "error:1001"
                String codeStr = fullKey.substring(REDIS_KEY_PREFIX.length()); // "1001"
                String value = redisTemplate.opsForValue().get(fullKey);
                if (value != null && !value.isBlank()) {
                    result.put(codeStr, value);
                }
            }
        } catch (Exception e) {
            log.error("[ErrorMessageRegistry] Failed to scan Redis keys: {}", e.getMessage(), e);
        }

        return result;
    }
}
