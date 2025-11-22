package com.mall.config.wx;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class WxStateStore {
    private final Cache<String, Boolean> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(10_000)
            .build();

    public String newState() {
        String s = UUID.randomUUID().toString().replace("-", "");
        cache.put(s, Boolean.TRUE);
        return s;
    }

    public boolean consume(String state) {
        if (state == null) return false;
        Boolean ok = cache.getIfPresent(state);
        if (ok != null && ok) {
            cache.invalidate(state);
            return true;
        }
        return false;
    }
}
