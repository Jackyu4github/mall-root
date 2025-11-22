package com.mall.config.wx;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wechat")
public class WechatProps {
    private String appId;
    private String appSecret;
    private String redirectUri;
    private String frontendCallback; // 可选
    private String apiV3Key; // 32 字节字符串
    private String mpAppId;
    private String mpSecret;

}
