package com.mall.notice.config;

import com.mall.auth.JwtCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.WebFilter;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtCodec jwtCodec;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/ws/notify").authenticated()
                        .anyExchange().permitAll()
                )
                .addFilterAt(jwtAuthWebFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public WebFilter jwtAuthWebFilter() {
        return (exchange, chain) -> {
            String token = extractToken(exchange.getRequest());
            if (token == null) return chain.filter(exchange);

            var authUser = jwtCodec.parseAccessAuthUser(token);
            if (authUser == null) return chain.filter(exchange);

            var auth = new UsernamePasswordAuthenticationToken(
                    authUser, null, List.of()
            );
            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
        };
    }

    private String extractToken(ServerHttpRequest request) {
        // 1) 尝试 header: Authorization: Bearer xxx
        List<String> authHeaders = request.getHeaders().getOrEmpty(HttpHeaders.AUTHORIZATION);
        if (!authHeaders.isEmpty()) {
            String v = authHeaders.get(0);
            if (v.startsWith("Bearer ")) {
                return v.substring(7);
            }
        }
        // 2) 尝试 query param: ?token=xxx
        return request.getQueryParams().getFirst("token");
    }
}
