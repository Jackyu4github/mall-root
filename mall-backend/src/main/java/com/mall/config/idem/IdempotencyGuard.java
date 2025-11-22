package com.mall.config.idem;

import com.mall.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class IdempotencyGuard {

    private final JdbcTemplate jdbc;

    @Transactional
    public <T> T run(String scope, String key, Supplier<T> biz) {
        // 1) 尝试登记
        int inserted = jdbc.update("""
            INSERT INTO http_idempotency(scope, idem_key, status, created_at, updated_at)
            VALUES (?, ?, 'PENDING', NOW(), NOW())
            ON CONFLICT (scope, idem_key) DO NOTHING
        """, scope, key);

        if (inserted == 0) {
            // 已存在：要么并发中，要么已处理
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT status, result_json FROM http_idempotency WHERE scope=? AND idem_key=?",
                scope, key);
            String status = (String) row.get("status");
            if ("DONE".equals(status) && row.get("result_json") != null) {
                // 如果你在某些接口缓存返回体，这里可以直接反序列化后返回
                // 这里返回 null，由上层按需处理
                throw new BizException("DUPLICATE_REQUEST");
            } else {
                throw new BizException("DUPLICATE_REQUEST_IN_PROGRESS");
            }
        }

        // 2) 业务执行
        try {
            T ret = biz.get();
            jdbc.update("""
                UPDATE http_idempotency
                   SET status='DONE', updated_at=NOW()
                 WHERE scope=? AND idem_key=?
            """, scope, key);
            return ret;
        } catch (RuntimeException e) {
            jdbc.update("""
                UPDATE http_idempotency
                   SET status='FAILED', updated_at=NOW()
                 WHERE scope=? AND idem_key=?
            """, scope, key);
            throw e;
        }
    }
}
