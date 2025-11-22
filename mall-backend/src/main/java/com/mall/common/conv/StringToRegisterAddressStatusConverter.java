package com.mall.common.conv;

import com.mall.domain.register.RegisterAddressStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class StringToRegisterAddressStatusConverter implements Converter<String, RegisterAddressStatus> {
    @Override
    public RegisterAddressStatus convert(String source) {
        if (!StringUtils.hasText(source)) return null;
        return RegisterAddressStatus.valueOf(source.trim().toUpperCase());
    }
}