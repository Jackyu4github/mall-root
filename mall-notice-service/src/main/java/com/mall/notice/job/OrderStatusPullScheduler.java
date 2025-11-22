package com.mall.notice.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.mall.notice.config.NoticeProps;
import com.mall.notice.model.WsBroadcastPayload;
import com.mall.notice.redis.NoticeRedisPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusPullScheduler {

    private final WebClient webClient;
    private final NoticeProps props;
    private final NoticeRedisPublisher publisher;

    @Scheduled(fixedDelayString = "${notice.push.orders-interval-ms:5000}")
    public void pullOrdersAndPublish() {
        String url = props.getBackend().getBaseUrl() + props.getBackend().getOrdersPath();
        String token = props.getBackend().getServiceToken();

        webClient.get()
                .uri(url)
                .headers(h -> {
                    if (StringUtils.hasText(token)) {
                        h.setBearerAuth(token);
                    }
                })
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(3))
                .onErrorResume(e -> {
                    log.warn("[notice] pull orders failed: {}", e.toString());
                    return Mono.empty();
                })
                .subscribe(orderListJson -> {
                    WsBroadcastPayload payload = new WsBroadcastPayload();
                    payload.setTargetType("SYS_USER"); // 可选
                    payload.setUserId(null);           // ✅ null=广播到所有在线端
                    payload.setEventType("ORDER_STATUS_CHANGED");
                    payload.setBodyJson(orderListJson.toString());
                    payload.setTimestamp(System.currentTimeMillis());
                    payload.setEventId(UUID.randomUUID().toString());

                    publisher.publish(payload);
                });
    }
}
