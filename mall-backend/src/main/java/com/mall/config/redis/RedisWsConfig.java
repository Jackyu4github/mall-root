package com.mall.config.redis;

import com.mall.web.ws.WsRedisSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@RequiredArgsConstructor
public class RedisWsConfig {

    public static final String WS_BROADCAST_TOPIC = "ws.broadcast";

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public ChannelTopic wsBroadcastTopic() {
        return new ChannelTopic(WS_BROADCAST_TOPIC);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            WsRedisSubscriber wsRedisSubscriber,   // ✅ 注入订阅者
            ChannelTopic wsBroadcastTopic          // ✅ 注入 topic
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // ✅ 关键：注册监听器
        container.addMessageListener(wsRedisSubscriber, wsBroadcastTopic);

        return container;
    }
}
