package com.tf.backend.core.domain.auth;

import com.tf.backend.core.model.dto.TokenResponse;
import com.tf.backend.core.common.exception.TokenAuthenticationException;
import org.springframework.security.core.Authentication;

public interface AuthenticationService {

    /**
     * 根据 Access Token 构建 Spring Security 认证对象
     */
    Authentication buildAuthenticationFor(String token) throws TokenAuthenticationException;

    /**
     * 清理用户缓存（强制下线或权限变更）
     */
    void evictLoginUserCache(Long userId);

    /**
     * 核心升级：使用 Refresh Token 换取新的双 Token 对
     */
    TokenResponse refreshToken(String refreshToken) throws TokenAuthenticationException;
}
