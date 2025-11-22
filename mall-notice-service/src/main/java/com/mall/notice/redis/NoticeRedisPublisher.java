package com.mall.notice.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.notice.config.NoticeProps;
import com.mall.notice.model.WsBroadcastPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeRedisPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final NoticeProps props;

    public void publish(WsBroadcastPayload payload) {
        try {
            String topic = props.getRedis().getTopic();
            String json = objectMapper.writeValueAsString(payload);
            redisTemplate.convertAndSend(topic, json);
            log.debug("[notice-redis] published to {}: {}", topic, json);
        } catch (Exception e) {
            log.error("[notice-redis] publish error", e);
        }
    }
}
