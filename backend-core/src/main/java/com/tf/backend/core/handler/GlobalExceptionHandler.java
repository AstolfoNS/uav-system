package com.tf.backend.core.handler;

import com.tf.backend.core.common.enumeration.HttpCode;
import com.tf.backend.core.common.exception.BizException;
import com.tf.backend.core.common.exception.TokenAuthenticationException;
import com.tf.backend.core.common.response.R;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 拦截自定义业务异常 (BizException)
     */
    @ExceptionHandler(BizException.class)
    public R<Void> handleBizException(BizException e) {
        log.warn("业务逻辑异常: {}", e.getMessage());
        return R.failed(e.getCode(), e.getMessage(), null);
    }

    /**
     * 拦截 Token 认证异常
     */
    @ResponseStatus(HttpStatus.UNAUTHORIZED) // 协议层 HTTP 401
    @ExceptionHandler(TokenAuthenticationException.class)
    public R<Void> handleTokenException(TokenAuthenticationException e) {
        log.warn("认证凭证异常: {}", e.getMessage());
        // 业务层返回 HttpCode.UNAUTHORIZED (401) 或 TOKEN_INVALID (903)，同时覆盖为具体的错误消息
        return R.failed(HttpCode.UNAUTHORIZED.getCode(), e.getMessage(), null);
    }

    /**
     * 拦截 Spring Security 权限不足异常
     */
    @ResponseStatus(HttpStatus.FORBIDDEN) // 协议层 HTTP 403
    @ExceptionHandler(AccessDeniedException.class)
    public R<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        // 业务层使用 HttpCode.FORBIDDEN (403)
        return R.failed(HttpCode.FORBIDDEN);
    }

    /**
     * 拦截参数校验异常 (@RequestBody + @Validated 抛出的异常)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("参数校验失败: {}", e.getMessage());
        Map<String, String> errorDetails = new HashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errorDetails.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        // 业务层使用 HttpCode.VALIDATION_ERROR (901)
        return R.failedWithDetails(HttpCode.VALIDATION_ERROR, errorDetails);
    }

    /**
     * 拦截参数校验异常 (表单提交 / URL 参数绑定抛出的异常)
     */
    @ExceptionHandler({BindException.class, ConstraintViolationException.class})
    public R<Void> handleBindException(Exception e) {
        log.warn("参数绑定/校验异常: {}", e.getMessage());
        Map<String, String> errorDetails = new HashMap<>();

        if (e instanceof BindException bindException) {
            for (FieldError fieldError : bindException.getBindingResult().getFieldErrors()) {
                errorDetails.put(fieldError.getField(), fieldError.getDefaultMessage());
            }
        } else if (e instanceof ConstraintViolationException cvException) {
            for (ConstraintViolation<?> violation : cvException.getConstraintViolations()) {
                String path = violation.getPropertyPath().toString();
                String field = path.substring(path.lastIndexOf('.') + 1);
                errorDetails.put(field, violation.getMessage());
            }
        }
        // 业务层同样使用 HttpCode.VALIDATION_ERROR (901)
        return R.failedWithDetails(HttpCode.VALIDATION_ERROR, errorDetails);
    }

    /**
     * 拦截 404 (找不到路由)
     */
    @ResponseStatus(HttpStatus.NOT_FOUND) // 协议层 HTTP 404
    @ExceptionHandler(NoHandlerFoundException.class)
    public R<Void> handleNotFoundException(NoHandlerFoundException e) {
        log.warn("接口不存在: {} {}", e.getHttpMethod(), e.getRequestURL());
        // 业务层使用 HttpCode.NOT_FOUND (404)
        return R.failed(HttpCode.NOT_FOUND);
    }

    /**
     * 拦截 405 (请求方法不支持)
     */
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED) // 协议层 HTTP 405
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<Void> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方式不支持: {}", e.getMessage());
        // 业务层使用 HttpCode.METHOD_NOT_ALLOWED (405)
        return R.failed(HttpCode.METHOD_NOT_ALLOWED);
    }

    /**
     * 兜底拦截：所有未知的系统异常 (NullPointerException, SQLException 等)
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // 协议层 HTTP 500
    @ExceptionHandler(Exception.class)
    public R<Void> handleGlobalException(Exception e) {
        log.error("系统发生未捕获异常: {}", e.getMessage(), e);
        // 业务层使用 HttpCode.FAILED (500)
        return R.failed(HttpCode.FAILED);
    }
}
