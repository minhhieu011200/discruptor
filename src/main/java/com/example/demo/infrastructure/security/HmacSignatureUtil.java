package com.example.demo.infrastructure.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility để tính và xác minh chữ ký HMAC-SHA256 cho xác thực Server-to-Server.
 *
 * <h3>Signing payload format:</h3>
 * 
 * <pre>
 *   HTTP_METHOD\n
 *   REQUEST_PATH\n
 *   QUERY_STRING\n          ← query string raw (?symbol=X&qty=1); "" nếu không có
 *   TIMESTAMP_MS\n
 *   HEX_SHA256(request_body)
 * </pre>
 *
 * <p>
 * Signature = Base64( HMAC-SHA256(secretKey, payload) )
 * </p>
 *
 * <h3>Ví dụ sử dụng phía client (server gọi đến):</h3>
 * 
 * <pre>
 * // GET với query params
 * Map&lt;String, String&gt; headers = HmacSignatureUtil.generateHeaders(
 *         "service-a",
 *         "my-super-secret-key",
 *         "GET",
 *         "/api/v1/orders",
 *         "symbol=USDJPY&from=2024-01-01", // query string (không có dấu '?')
 *         null // body null với GET
 * );
 *
 * // POST với body
 * Map&lt;String, String&gt; headers = HmacSignatureUtil.generateHeaders(
 *         "service-a",
 *         "my-super-secret-key",
 *         "POST",
 *         "/api/v1/orders",
 *         "", // không có query
 *         "{\"symbol\":\"USDJPY\",\"qty\":1000}");
 * </pre>
 */
public final class HmacSignatureUtil {

    public static final String HEADER_KEY_ID = "X-Api-Key-Id";
    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    public static final String HEADER_SIGNATURE = "X-Signature";

    private static final String ALGORITHM = "HmacSHA256";

    private HmacSignatureUtil() {
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phía SERVER – xác minh chữ ký
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tính HMAC-SHA256 cho request và trả về Base64 string.
     *
     * @param secretKey   secret key (plain text)
     * @param method      HTTP method (GET, POST, …)
     * @param path        request path (không bao gồm query string, VD:
     *                    /api/v1/orders)
     * @param queryString raw query string KHÔNG có dấu '?' (VD:
     *                    symbol=USDJPY&qty=1000),
     *                    truyền "" hoặc null nếu không có
     * @param timestamp   Unix epoch milliseconds (String)
     * @param body        raw request body bytes (null/empty cho GET)
     * @return Base64-encoded HMAC-SHA256 signature
     */
    public static String computeSignature(String secretKey,
            String method,
            String path,
            String queryString,
            String timestamp,
            byte[] body) throws Exception {
        String bodySha256 = sha256Hex(body);
        String payload = buildPayload(method, path, queryString, timestamp, bodySha256);

        Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM));
        byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmacBytes);
    }

    /**
     * So sánh constant-time hai chữ ký để tránh timing attack.
     *
     * @return true nếu khớp
     */
    public static boolean verifySignature(String expected, String actual) {
        if (expected == null || actual == null)
            return false;
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phía CLIENT – tạo headers để gắn vào request
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tạo sẵn 3 headers S2S cần thiết để gắn vào HTTP request gọi đến server này.
     *
     * @param keyId       key-id (phải trùng key đã config bên server)
     * @param secretKey   secret key tương ứng với keyId
     * @param method      HTTP method
     * @param path        request path (không có query string)
     * @param queryString query string không có '?' (VD: "symbol=USDJPY&qty=1000"),
     *                    "" nếu không có
     * @param body        request body bytes (null nếu không có)
     * @return map chứa 3 headers: X-Api-Key-Id, X-Timestamp, X-Signature
     */
    public static Map<String, String> generateHeaders(String keyId,
            String secretKey,
            String method,
            String path,
            String queryString,
            byte[] body) throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String signature = computeSignature(secretKey, method, path, queryString, timestamp, body);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(HEADER_KEY_ID, keyId);
        headers.put(HEADER_TIMESTAMP, timestamp);
        headers.put(HEADER_SIGNATURE, signature);
        return headers;
    }

    /**
     * Convenience overload nhận body dạng String.
     *
     * <pre>
     * // GET request:
     * generateHeaders("service-a", secret, "GET", "/api/v1/quotes", "symbol=USDJPY", null)
     *
     * // POST request:
     * generateHeaders("service-a", secret, "POST", "/api/v1/orders", "", "{\"qty\":1000}")
     * </pre>
     */
    public static Map<String, String> generateHeaders(String keyId,
            String secretKey,
            String method,
            String path,
            String queryString,
            String body) throws Exception {
        byte[] bodyBytes = (body == null || body.isEmpty())
                ? null
                : body.getBytes(StandardCharsets.UTF_8);
        return generateHeaders(keyId, secretKey, method, path, queryString, bodyBytes);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Payload = METHOD\nPATH\nQUERY_STRING\nTIMESTAMP\nBODY_SHA256_HEX
     *
     * <ul>
     * <li>QUERY_STRING: raw query string không có '?', "" nếu không có</li>
     * <li>Thứ tự query params phải giữ nguyên so với URL gốc để signature khớp</li>
     * </ul>
     */
    public static String buildPayload(String method,
            String path,
            String queryString,
            String timestamp,
            String bodySha256Hex) {
        String q = (queryString == null) ? "" : queryString;
        return method + "\n" + path + "\n" + q + "\n" + timestamp + "\n" + bodySha256Hex;
    }

    /**
     * SHA-256 của byte array, trả về hex lowercase.
     * Nếu body null/empty trả về SHA-256 của chuỗi rỗng.
     */
    public static String sha256Hex(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = (data == null || data.length == 0)
                ? digest.digest(new byte[0])
                : digest.digest(data);
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
