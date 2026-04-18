package com.tf.backend.core.config.resolver;

import com.tf.backend.core.common.annotation.ExtractRefreshToken;
import com.tf.backend.core.common.enumeration.HttpCode;
import com.tf.backend.core.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class RefreshTokenArgumentResolver implements HandlerMethodArgumentResolver {

    /**
     * 第一关：判断拦截条件
     * 只有当参数上标注了 @ExtractRefreshToken，并且参数类型是 String 时，才归我管。
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(ExtractRefreshToken.class) && parameter.getParameterType().equals(String.class);
    }

    /**
     * 第二关：核心解析逻辑
     * 如果 supportsParameter 返回 true，Spring 就会执行这个方法来获取具体的参数值。
     */
    @Override
    public Object resolveArgument(
            @NonNull MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        // 获取原生的 HttpServletRequest
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            return null;
        }
        // 提取 Header 并截取 "Bearer "
        String headerValue = request.getHeader("Refresh-Token");
        String token = null;
        if (StringUtils.hasText(headerValue) && headerValue.startsWith("Bearer ")) {
            token = headerValue.substring(7); // 截掉前7个字符 "Bearer "
        }
        // 拿到参数上的注解实例，判断是否为必传项
        ExtractRefreshToken annotation = parameter.getParameterAnnotation(ExtractRefreshToken.class);
        
        if (annotation != null && annotation.required() && !StringUtils.hasText(token)) {
            // 如果 required=true，但 token 是空的，直接抛出 BizException
            // 这个异常会被你昨天写的 GlobalExceptionHandler 完美拦截，并返回给前端 901 或 400 错误
            throw new BizException(HttpCode.BAD_REQUEST.getCode(), "缺失有效的 Refresh Token");
        }

        // 返回最终干干净净的 Token 字符串，它会被自动注入到 Controller 的方法参数中
        return token;
    }
}