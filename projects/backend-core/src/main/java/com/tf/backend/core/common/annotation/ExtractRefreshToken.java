package com.tf.backend.core.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记在 Controller 方法参数上，自动从 Header 中提取 Refresh-Token 并去除 Bearer 前缀
 */
@Target(ElementType.PARAMETER) // 表明这个注解只能用在方法参数上
@Retention(RetentionPolicy.RUNTIME) // 运行时生效
public @interface ExtractRefreshToken {

    /**
     * 是否必传。
     * 如果为 true，当 Header 中没有 Refresh-Token 时，会自动抛出参数缺失异常。
     * 默认为 true。
     */
    boolean required() default true;
}