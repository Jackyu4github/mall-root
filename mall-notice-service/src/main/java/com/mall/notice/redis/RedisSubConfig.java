package com.mall.notice.redis;

import com.mall.notice.config.NoticeProps;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

@Configuration
@RequiredArgsConstructor
public class RedisSubConfig {

    private final RedisConnectionFactory connectionFactory;
    private final NoticeRedisSubscriber subscriber;
    private final NoticeProps props;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        String topic = props.getRedis().getTopic();
        container.addMessageListener((message, pattern) -> {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            subscriber.onMessage(body);
        }, new PatternTopic(topic));

        return container;
    }
}
