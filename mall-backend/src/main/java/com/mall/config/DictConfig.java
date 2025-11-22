package com.mall.config;

import com.mall.service.dict.DictProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DictProperties.class)
public class DictConfig { }
