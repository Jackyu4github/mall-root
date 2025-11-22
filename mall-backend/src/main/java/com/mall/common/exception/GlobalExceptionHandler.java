package com.mall.common.exception;

import com.mall.common.api.ApiResponse;
import com.mall.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolationException;
import java.util.Locale;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    // ========= 1. 业务异常（你原来的） =========
    @ExceptionHandler(BizException.class)
    public ApiResponse<?> handleBiz(BizException ex, Locale locale) {
        ErrorCode code = ex.getErrorCode() != null
                ? ex.getErrorCode()
                : ErrorCode.BIZ_FALLBACK;

        String msg = resolveMessage(code, ex, locale);

        log.warn("[BizException] code={}, msg={}, raw={}", code.getCode(), msg, ex.getMessage());

        return ApiResponse.error(code, msg);
    }

    // ========= 2. 参数校验异常（A0400）=========

    // @RequestBody + @Valid 校验失败
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<?> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, Locale locale) {
        ErrorCode code = ErrorCode.REQUEST_INVALID;

        // 可选：从第一个错误里拿点信息，方便排查
        String detail = null;
        if (ex.getBindingResult() != null && ex.getBindingResult().hasErrors()) {
            var fieldError = ex.getBindingResult().getFieldError();
            if (fieldError != null) {
                detail = fieldError.getField() + " " + fieldError.getDefaultMessage();
            }
        }
        log.warn("[RequestInvalid] {}", detail, ex);

        String msg = resolveMessage(code, ex, locale);
        return ApiResponse.error(code, msg);
    }

    // 表单 / Query 参数绑定失败
    @ExceptionHandler(BindException.class)
    public ApiResponse<?> handleBindException(BindException ex, Locale locale) {
        ErrorCode code = ErrorCode.REQUEST_INVALID;

        log.warn("[BindException] {}", ex.getMessage(), ex);

        String msg = resolveMessage(code, ex, locale);
        return ApiResponse.error(code, msg);
    }

    // 单参数校验（如 @RequestParam @PathVariable 上的 @Min @NotBlank）
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<?> handleConstraintViolation(ConstraintViolationException ex, Locale locale) {
        ErrorCode code = ErrorCode.REQUEST_INVALID;

        log.warn("[ConstraintViolation] {}", ex.getMessage(), ex);

        String msg = resolveMessage(code, ex, locale);
        return ApiResponse.error(code, msg);
    }

    // ========= 3. 权限不足（A0403）=========

    @ExceptionHandler(AccessDeniedException.class)
    public ApiResponse<?> handleAccessDenied(AccessDeniedException ex, Locale locale) {
        ErrorCode code = ErrorCode.FORBIDDEN;

        log.warn("[Forbidden] {}", ex.getMessage());

        String msg = resolveMessage(code, ex, locale);
        return ApiResponse.error(code, msg);
    }

    // ========= 4. 资源 / 接口相关 =========

    // Spring 6 之后的 404 静态资源 / 接口不存在
    @ExceptionHandler(NoResourceFoundException.class)
    public ApiResponse<?> handleNoResource(NoResourceFoundException ex, Locale locale) {
        ErrorCode code = ErrorCode.NOT_FOUND;

        log.warn("[NotFound] {}", ex.getMessage());

        String msg = resolveMessage(code, ex, locale);
        return ApiResponse.error(code, msg);
    }

    // HTTP 方法不允许（比如前端用 POST 调了 GET 接口）
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResponse<?> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                   Locale locale) {
        ErrorCode code = ErrorCode.METHOD_NOT_ALLOWED;

        log.warn("[MethodNotAllowed] {}", ex.getMessage());

        String msg = resolveMessage(code, ex, locale);
        return ApiResponse.error(code, msg);
    }

    // ========= 5. 数据库约束 / 冲突（A0501）=========

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ApiResponse<?> handleDataIntegrity(DataIntegrityViolationException ex, Locale locale) {
        ErrorCode code = ErrorCode.DB_CONFLICT;

        // 这里通常是唯一键冲突 / 非空约束 / 外键约束等
        log.warn("[DBConflict] {}", ex.getMostSpecificCause().getMessage(), ex);

        String msg = resolveMessage(code, ex, locale);
        return ApiResponse.error(code, msg);
    }

    // ========= 6. 兜底系统异常（A0001）=========

    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handleGeneric(Exception ex, Locale locale) {
        log.error("[SystemException]", ex);

        ErrorCode code = ErrorCode.SYSTEM_ERROR;
        String msg = resolveMessage(code, ex, locale);

        return ApiResponse.error(code, msg);
    }

    // ========= 公共 i18n 解析 =========

    private String resolveMessage(ErrorCode code, Exception ex, Locale locale) {
        try {
            Object[] args = (ex instanceof BizException biz) ? biz.getArgs() : null;

            return messageSource.getMessage(
                    code.getMsgKey(),
                    args,
                    code.getDefaultMsg(),   // 默认用枚举里的默认文案
                    locale
            );
        } catch (Exception ignore) {
            return code.getDefaultMsg();
        }
    }
}
