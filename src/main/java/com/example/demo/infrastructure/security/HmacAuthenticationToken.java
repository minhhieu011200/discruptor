package com.example.demo.infrastructure.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * Spring Security Authentication token đại diện cho một S2S request
 * đã được xác thực thành công qua HMAC-SHA256.
 *
 * <p>Principal = keyId (tên service đã được xác thực).
 * Authority = ROLE_SERVICE.</p>
 */
public class HmacAuthenticationToken extends AbstractAuthenticationToken {

    private final String keyId;

    public HmacAuthenticationToken(String keyId) {
        super(List.of(new SimpleGrantedAuthority("ROLE_SERVICE")));
        this.keyId = keyId;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null; // Không lưu secret sau khi đã xác thực
    }

    @Override
    public Object getPrincipal() {
        return keyId;
    }

    @Override
    public String getName() {
        return keyId;
    }
}
