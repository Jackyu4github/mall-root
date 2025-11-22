package com.mall.notice.config;

import com.mall.auth.JwtCodec;
import com.mall.auth.JwtProps;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({JwtProps.class, NoticeProps.class})
public class JwtConfig {

    @Bean
    public JwtCodec jwtCodec(JwtProps props) {
        // expireSeconds 在 notice 只用于解析，不生成 token
        long accessExp = props.getExpireSeconds(); // 你也可以直接写 8h
        long refreshExp = 30L * 24 * 60 * 60;     // 随便给个一致值即可
        return new JwtCodec(props.getSecret(), props.getIssuer(), accessExp, refreshExp);
    }
}
