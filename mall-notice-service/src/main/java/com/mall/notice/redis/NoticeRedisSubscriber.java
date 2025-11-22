package com.mall.notice.redis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.notice.model.NotifyMessage;
import com.mall.notice.model.WsBroadcastPayload;
import com.mall.notice.ws.UserNotifyWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeRedisSubscriber {

    private final ObjectMapper objectMapper;
    private final UserNotifyWebSocketHandler wsHandler;

    // 被 RedisMessageListenerContainer 调用
    public void onMessage(String message) {
        try {
            WsBroadcastPayload payload = objectMapper.readValue(message, WsBroadcastPayload.class);

            JsonNode bodyNode;
            try {
                bodyNode = objectMapper.readTree(payload.getBodyJson());
            } catch (Exception ignore) {
                bodyNode = objectMapper.valueToTree(payload.getBodyJson());
            }

            NotifyMessage nm = new NotifyMessage(payload.getEventType(), bodyNode);

            if (payload.getUserId() != null) {
                wsHandler.sendToUser(payload.getUserId(), nm);
            } else {
                wsHandler.broadcast(nm);
            }

        } catch (Exception e) {
            log.warn("[notice-redis] parse/push error, raw={}", message, e);
        }
    }
}
