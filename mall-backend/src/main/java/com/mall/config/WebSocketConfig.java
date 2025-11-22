package com.mall.config;


import com.mall.common.security.JwtHandshakeInterceptor;
import com.mall.web.ws.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationWebSocketHandler, "/ws/notify")
                // 先过我们自定义的 JWT 校验
                .addInterceptors(jwtHandshakeInterceptor)
                // 允许跨域（开发阶段先全放开，生产你可以按域名细化）
                .setAllowedOriginPatterns("*");
    }
}
