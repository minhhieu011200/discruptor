package com.example.demo.application.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties cho xác thực Server-to-Server HMAC-SHA256.
 *
 * <pre>
 * security.s2s.enabled=true
 * security.s2s.keys.service-a=secret-key-a
 * security.s2s.clock-skew-seconds=300
 * security.s2s.permit-paths=/actuator/**,/swagger-ui/**
 * </pre>
 */

@Configuration
@ConfigurationProperties(prefix = "security.s2s")
@Data
public class S2sSecurityProperties {

    /**
     * Bật / tắt toàn bộ xác thực S2S.
     * Khi {@code false}, tất cả request đều được pass-through mà không cần ký.
     */
    private boolean enabled = true;

    /**
     * Map key-id → secret.
     * Ví dụ: service-a=my-super-secret-key
     */
    private Map<String, String> keys = new HashMap<>();

    /**
     * Khoảng thời gian cho phép lệch đồng hồ giữa các server (giây).
     * Request có timestamp ngoài khoảng [now - skew, now + skew] sẽ bị từ chối.
     */
    private long clockSkewSeconds = 300L;

    /**
     * Danh sách Ant path patterns không cần xác thực.
     * Ví dụ: /actuator/**, /swagger-ui/**
     */
    private List<String> permitPaths = List.of(
            "/actuator/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/webjars/**");
}
