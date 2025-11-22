package com.mall.common.api;

import com.mall.common.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponse<T> {

    /**
     * 业务码，如：
     * - 000000：成功
     * - A0101：TOKEN 无效
     * - B00001：业务兜底错误
     */
    private String code;

    /**
     * 展示给前端的提示文字（已做 i18n 解析后）
     */
    private String msg;

    private T data;

    // ======= 成功 =======

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), "ok", data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), "ok", null);
    }

    // ======= 错误（推荐新用法）=======

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String msg) {
        return new ApiResponse<>(errorCode.getCode(), msg, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.name(), null);
    }

    // ======= 兼容旧用法：只有 msg，没有 code =======
    // 会统一映射为 B00001（业务兜底错误）
    public static <T> ApiResponse<T> error(String msg) {
        return new ApiResponse<>(ErrorCode.BIZ_FALLBACK.getCode(), msg, null);
    }
}
