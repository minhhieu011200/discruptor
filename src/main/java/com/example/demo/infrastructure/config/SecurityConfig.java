package com.example.demo.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

import com.example.demo.application.properties.S2sSecurityProperties;

/**
 * Spring Security configuration cho WebFlux.
 *
 * <ul>
 * <li>Disable CSRF – không cần thiết cho S2S REST API</li>
 * <li>Disable HTTP Basic / Form Login</li>
 * <li>Permit actuator, swagger, api-docs không cần auth</li>
 * <li>Mọi path còn lại yêu cầu authenticated (được xử lý bởi
 * {@code HmacAuthFilter})</li>
 * </ul>
 *
 * <p>
 * Khi {@code security.s2s.enabled=false}, HmacAuthFilter tự bỏ qua nên
 * Spring Security sẽ cho phép tất cả request đi qua (vì filter không set
 * SecurityContext)
 * → Có thể kết hợp với {@code permitAll()} khi disabled.
 * </p>
 */
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final S2sSecurityProperties s2sProps;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        // Lấy permit-paths từ config
        String[] permitPaths = s2sProps.getPermitPaths()
                .stream()
                .toArray(String[]::new);

        http
                // Disable CSRF – REST API không cần
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // Disable HTTP Basic authentication
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)

                // Disable form-based login
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                // Không lưu SecurityContext vào session – stateless S2S
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())

                // Authorization rules
                .authorizeExchange(exchanges -> {
                    // Permit-paths (actuator, swagger, ...)
                    exchanges.pathMatchers(permitPaths).permitAll();

                    if (s2sProps.isEnabled()) {
                        // S2S enabled → yêu cầu authentication cho tất cả còn lại
                        // (HmacAuthFilter sẽ inject HmacAuthenticationToken)
                        exchanges.anyExchange().authenticated();
                    } else {
                        // S2S disabled → cho phép tất cả (không cần ký)
                        exchanges.anyExchange().permitAll();
                    }
                });

        return http.build();
    }
}
