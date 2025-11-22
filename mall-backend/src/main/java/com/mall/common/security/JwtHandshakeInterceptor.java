package com.mall.common.security;

import com.mall.common.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

/**
 * 在 WebSocket 握手阶段做鉴权：
 * 1. 从 ?token=... 或 Authorization: Bearer ... 里拿 JWT
 * 2. 用 JwtUtil 解析
 * 3. 如果合法，把解析结果塞到 attributes 传给后面的 WebSocketSession
 * 4. 不合法就 return false -> 握手失败，连接直接被拒绝
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        // 1. 优先从 query 参数里拿 token
        String token = extractTokenFromQuery(request.getURI());

        // 2. 如果 query 没有，就看看 Header: Authorization: Bearer xxx
        if (token == null) {
            token = extractTokenFromHeader(request);
        }

        if (token == null || token.isBlank()) {
            log.warn("[WS] handshake rejected: no token");
            return false;
        }

        // 3. 解析 JWT
        var authInfo = jwtUtil.parseToken(token);
        if (authInfo == null) {
            log.warn("[WS] handshake rejected: invalid token={}", token);
            return false;
        }

        // 4. 通过校验，把登录信息塞进 attributes
        //    之后可以从 WebSocketSession.getAttributes() 里拿到
        attributes.put("authInfo", authInfo);

        log.info("[WS] handshake ok. user={}", authInfo);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // 不需要做什么
    }

    private String extractTokenFromQuery(URI uri) {
        String query = uri.getQuery(); // 形如 "token=xxxxx&foo=bar"
        if (query == null) return null;

        for (String kv : query.split("&")) {
            int p = kv.indexOf('=');
            if (p <= 0) continue;
            String key = kv.substring(0, p);
            String val = kv.substring(p + 1);
            if ("token".equals(key)) {
                return val;
            }
        }
        return null;
    }

    private String extractTokenFromHeader(ServerHttpRequest request) {
        var headers = request.getHeaders();
        var authHeader = headers.getFirst("Authorization");
        if (authHeader == null) return null;
        if (!authHeader.startsWith("Bearer ")) return null;
        return authHeader.substring("Bearer ".length()).trim();
    }
}
