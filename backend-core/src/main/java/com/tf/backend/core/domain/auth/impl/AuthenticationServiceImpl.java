package com.tf.backend.core.domain.auth.impl;

import com.tf.backend.core.common.constant.AuthConst;
import com.tf.backend.core.common.enumeration.HttpCode;
import com.tf.backend.core.common.enumeration.PermType;
import com.tf.backend.core.domain.auth.AuthenticationService;
import com.tf.backend.core.model.dto.TokenResponse;
import com.tf.backend.core.common.exception.TokenAuthenticationException;
import com.tf.backend.core.infrastructure.cache.RedisManager;
import com.tf.backend.core.model.entity.UserEntity;
import com.tf.backend.core.infrastructure.repo.PermissionService;
import com.tf.backend.core.infrastructure.repo.RoleService;
import com.tf.backend.core.infrastructure.repo.UserService;
import com.tf.backend.core.security.AuthenticationToken;
import com.tf.backend.core.security.JwtProvider;
import com.tf.backend.core.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserService userService;

    private final RoleService roleService;

    private final PermissionService permissionService;

    private final JwtProvider jwtProvider;

    private final RedisManager redisManager;


    @Override
    public Authentication buildAuthenticationFor(String token) throws TokenAuthenticationException {
        try {
            Long userId = jwtProvider.getUserId(token);
            return buildAuthentication(userId, token, PermType.API);
        } catch (JwtException e) {
            throw new TokenAuthenticationException("Access Token 无效或已过期", e);
        }
    }

    public Authentication buildAuthentication(Long userId, String token, PermType type) throws TokenAuthenticationException {
        LoginUser loginUser = redisManager.getOrSet(
                buildLoginUserCacheKey(userId, type),
                LoginUser.class,
                () -> loadLoginUserFromDB(userId, type),
                1, TimeUnit.HOURS
        );
        if (loginUser == null) {
            throw new TokenAuthenticationException("用户不存在或已被禁用: " + userId);
        }
        return new AuthenticationToken(loginUser, token);
    }

    /**
     * 核心逻辑：Token 刷新与轮换
     */
    @Override
    public TokenResponse refreshToken(String refreshToken) throws TokenAuthenticationException {
        try {
            // 解析并验证旧 RT 的签名与有效期
            Jwt jwt = jwtProvider.decode(refreshToken);
            Long userId = jwtProvider.getUserId(jwt);
            String jti = jwtProvider.getJti(jwt);

            if (!StringUtils.hasText(jti)) {
                throw new TokenAuthenticationException(HttpCode.TOKEN_INVALID.getMessage());
            }
            // 数据库/Redis 状态校验：检查此旧 jti 是否被吊销
            String rtKey = buildRefreshTokenCacheKey(userId, jti);
            if (!redisManager.hasKey(rtKey)) {
                // 如果命中此处，说明发生了 Token 窃取重放，或者是服务端主动踢下线
                log.warn("检测到失效的 Refresh Token 尝试刷新! userId: {}, jti: {}", userId, jti);
                throw new TokenAuthenticationException("登录状态已失效，请重新登录");
            }
            // 推断“记住我”状态 (继承之前的选择)
            // 通过对比签发时间(IssuedAt)和过期时间(ExpiresAt)的差值来判断原始寿命
            boolean isRememberMe = false;
            if (jwt.getExpiresAt() != null && jwt.getIssuedAt() != null) {
                isRememberMe = jwt.getExpiresAt().toEpochMilli() - jwt.getIssuedAt().toEpochMilli() > TimeUnit.HOURS.toMillis(1);
            }
            // 执行轮换：废弃旧 RT，保证它只能使用一次
            redisManager.delete(rtKey);

            // 生成全新的双 Token
            String newAT = jwtProvider.generateAccessToken(userId);
            String newRT = jwtProvider.generateRefreshToken(userId, isRememberMe);

            // 解析新的 RT 获取新的 jti 和精准的实际过期时间，保证 Redis 与 Token 存活期完全一致
            Jwt newRtJwt = jwtProvider.decode(newRT);

            assert newRtJwt.getExpiresAt() != null;

            long realRtExpireMillis = newRtJwt.getExpiresAt().toEpochMilli() - System.currentTimeMillis();
            // 持久化新 RT 标识到 Redis
            redisManager.set(
                    buildRefreshTokenCacheKey(userId, jwtProvider.getJti(newRtJwt)),
                    "1",
                    realRtExpireMillis,
                    TimeUnit.MILLISECONDS
            );
            // 异步更新最后登录时间
            userService.updateLastLoginTimeAsync(userId);

            return new TokenResponse(
                    newAT,
                    newRT,
                    jwtProvider.getProps().getExpire() / 1000
            );
        } catch (JwtException e) {
            // 专门捕获底层 jwtProvider 解析时抛出的异常 (过期、签名不对、被篡改等)
            throw new TokenAuthenticationException("Refresh Token 已过期或非法", e);
        }
    }

    private LoginUser loadLoginUserFromDB(Long userId, PermType type) {
        // 优化：返回 null 触发 RedisManager 写入负缓存，防止恶意 userId 穿透打爆数据库
        UserEntity user = userService.getOptById(userId).orElse(null);
        if (user == null) {
            return null;
        }
        return LoginUser.from(
                user,
                roleService.getByUserId(userId),
                permissionService.getByUserIdAndPermType(userId, type)
        );
    }

    @Override
    public void evictLoginUserCache(Long userId) {
        if (userId == null) {
            return;
        }
        for (PermType type : PermType.values()) {
            redisManager.delete(buildLoginUserCacheKey(userId, type));
            redisManager.delete("NULL_" + buildLoginUserCacheKey(userId, type));
        }
    }

    private String buildLoginUserCacheKey(Long userId, PermType type) {
        return AuthConst.LOGIN_USER_CACHE_PREFIX + userId + ":" + type.name();
    }

    private String buildRefreshTokenCacheKey(Long userId, String jti) {
        return AuthConst.REFRESH_TOKEN_CACHE_PREFIX + userId + ":" + jti;
    }
}
