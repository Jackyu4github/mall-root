package com.mall.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProps {
    private String secret;
    private long expireSeconds;
    private String issuer; // 你有就留，没有可以不写
}
