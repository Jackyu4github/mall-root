package com.mall.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// ↓↓↓ 新增 import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                String token = auth.substring("Bearer ".length()).trim();
                var info = jwtUtil.parseToken(token);
                if (info != null) {
                    // 1) 继续保留你现有的 ThreadLocal（兼容业务层）
                    AuthContext.set(info);

                    // 2) 关键补丁：把认证写入 Spring Security 的上下文
                    //    你的 token 里有 role，例如 "SUPER_ADMIN"
                    String role = info.getRole();
                    List<SimpleGrantedAuthority> authorities =
                            (role == null || role.isBlank())
                                    ? List.of()
                                    : List.of(new SimpleGrantedAuthority("ROLE_" + role));

                    var authentication = new UsernamePasswordAuthenticationToken(
                            info,                  // principal：直接放 UserInfo
                            null,                  // credentials
                            authorities            // 授权
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            // 防止线程复用串号
            SecurityContextHolder.clearContext();
            AuthContext.clear();
        }
    }

    // （可选）放行无需鉴权的路径，减少无效解析
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        // Auth & Docs
        if (p.startsWith("/admin/v1/auth/")) return true;
        if (p.startsWith("/api/v1/auth/"))   return true;
        if (p.startsWith("/v3/api-docs"))    return true;
        if (p.startsWith("/swagger-ui"))     return true;

        // Actuator / Prometheus
        if (p.equals("/actuator/prometheus")) return true;
        if (p.startsWith("/actuator"))        return true;

        // WebSocket & Static
        if (p.startsWith("/ws/"))     return true;
        if (p.startsWith("/public/")) return true;
        if (p.startsWith("/files/"))  return true;

        return false;
    }
}
