package com.mall.common.exception;

import com.mall.common.error.ErrorCode;
import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] args; // 预留给国际化占位符

    // ===== 推荐新用法：直接传 ErrorCode =====
    public BizException(ErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.args = null;
    }

    public BizException(ErrorCode errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }

    // ===== 兼容旧用法：只传 msg，默认 BIZ_FALLBACK =====
    public BizException(String msg) {
        super(msg);
        this.errorCode = ErrorCode.BIZ_FALLBACK;
        this.args = null;
    }
}
