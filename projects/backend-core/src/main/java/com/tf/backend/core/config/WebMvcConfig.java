package com.tf.backend.core.config;

import com.tf.backend.core.config.resolver.RefreshTokenArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@RequiredArgsConstructor
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RefreshTokenArgumentResolver refreshTokenArgumentResolver;

    /**
     * 将自定义的参数解析器添加到 Spring MVC 的解析器链条中
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(refreshTokenArgumentResolver);
    }
}
