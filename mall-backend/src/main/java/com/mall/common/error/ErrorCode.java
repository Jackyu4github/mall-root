package com.mall.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 通用错误码约定：
 * - 成功：000000
 * - 系统级：A****
 * - 业务级：B****
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ===== 成功 =====
    SUCCESS("000000", "success.ok", "成功"),

    // ===== 系统级错误 A**** =====
    SYSTEM_ERROR("A0001", "error.system", "系统繁忙，请稍后重试"),

    TOKEN_INVALID("A0101", "error.token.invalid", "访问令牌无效或过期"),
    TOKEN_REFRESH_INVALID("A0102", "error.token.refresh_invalid", "刷新令牌无效或过期"),

    // 参数相关
    REQUEST_INVALID("A0400", "error.request.invalid", "请求参数不合法"),

    // 鉴权/权限
    FORBIDDEN("A0403", "error.auth.forbidden", "无权限访问该资源"),

    // 资源/接口相关
    NOT_FOUND("A0404", "error.request.not_found", "请求资源不存在"),
    METHOD_NOT_ALLOWED("A0405", "error.request.method_not_allowed", "请求方法不被允许"),

    // 数据库相关
    DB_CONFLICT("A0501", "error.db.conflict", "数据冲突或约束违规"),

    // ===== 业务级错误 B**** =====
    BIZ_FALLBACK("B00001", "biz.error.fallback", ""),

    BIZ_ADDRESS_NOT_EXIST("B0201", "biz.error.address.not.exist", ""),

    BIZ_REGISTER_NAME_NOT_MATCH("B0202", "biz.error.register.name.not.match", ""),

    BIZ_PRODUCT_NOT_EXIST("B0204", "biz.error.product.not.exist", ""),

    BIZ_PRODUCT_INSUFFICIENT_STOCK("B0205", "biz.error.product.insufficient", ""),

    BIZ_ORDER_ID_NOT_EXIST("B0301", "biz.error.order.id.not.exist", ""),

    BIZ_PAY_NO_NOT_EXIST("B0302", "biz.error.pay.no.not.exist", ""),

    ;

    private final String code;

    /**
     * 国际化 message key，如 messages_zh_CN.properties 里的 key
     */
    private final String msgKey;

    private final String defaultMsg;
}
