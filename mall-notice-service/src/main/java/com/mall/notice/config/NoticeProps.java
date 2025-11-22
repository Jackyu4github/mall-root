package com.mall.notice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "notice")
public class NoticeProps {

    private Backend backend = new Backend();
    private Push push = new Push();
    private Redis redis = new Redis();

    @Data
    public static class Backend {
        private String baseUrl;
        private String ordersPath;
        private String serviceToken;
    }

    @Data
    public static class Push {
        private long ordersIntervalMs = 5000;
    }

    @Data
    public static class Redis {
        private String topic = "user.notify";
    }
}
