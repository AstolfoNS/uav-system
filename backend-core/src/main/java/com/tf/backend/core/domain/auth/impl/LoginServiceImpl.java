package com.tf.backend.core.domain.auth.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tf.backend.core.common.constant.AuthConst;
import com.tf.backend.core.common.enumeration.HttpCode;
import com.tf.backend.core.common.enumeration.Status;
import com.tf.backend.core.domain.auth.AuthenticationService;
import com.tf.backend.core.domain.auth.LoginService;
import com.tf.backend.core.model.dto.LoginRequest;
import com.tf.backend.core.model.dto.TokenResponse;
import com.tf.backend.core.common.exception.BizException;
import com.tf.backend.core.infrastructure.cache.RedisManager;
import com.tf.backend.core.model.entity.UserEntity;
import com.tf.backend.core.infrastructure.repo.UserService;
import com.tf.backend.core.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class LoginServiceImpl implements LoginService {

    private final UserService userService;

    private final PasswordEncoder passwordEncoder;

    private final JwtProvider jwtProvider;

    private final RedisManager redisManager;

    private final AuthenticationService authenticationService;


    @Override
    public TokenResponse login(LoginRequest request) {
        // 查库：根据用户名查找用户
        UserEntity user = userService.getOne(
                Wrappers.<UserEntity>lambdaQuery().eq(UserEntity::getUsername, request.username())
        );
        // 校验账号是否存在与密码是否匹配（统一抛出模糊异常，防止暴力撞库探测）
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BizException(HttpCode.PERMISSION_DENIED);
        }
        if (user.getStatus() != Status.ENABLED) {
            throw new BizException(910, "账号状态异常，请联系管理员");
        }
        // 生成双 Token (传入 rememberMe 状态以动态决定 RT 寿命)
        String newAT = jwtProvider.generateAccessToken(user.getId());
        String newRT = jwtProvider.generateRefreshToken(user.getId(), request.rememberMe());

        // 解析刚生成的 RT，获取精准的过期时间和 jti
        Jwt rtJwt = jwtProvider.decode(newRT);
        String jti = jwtProvider.getJti(rtJwt);

        assert rtJwt.getExpiresAt() != null;
        // 获取真实的到期时间（毫秒），以便配置给 Redis
        long realRtExpireMillis = rtJwt.getExpiresAt().toEpochMilli() - System.currentTimeMillis();

        // 将 RT 存入 Redis，建立设备会话 (使用真实计算出的 TTL)
        String rtKey = AuthConst.REFRESH_TOKEN_CACHE_PREFIX + user.getId() + ":" + jti;
        redisManager.set(
                rtKey,
                "1",
                realRtExpireMillis,
                TimeUnit.MILLISECONDS
        );
        // 异步更新最后登录时间 (方案 A，调用 dbExecutor 线程池)
        userService.updateLastLoginTimeAsync(user.getId());

        log.info("用户登录成功: userId={}, username={}, rememberMe={}", user.getId(), user.getUsername(), request.rememberMe());

        return new TokenResponse(newAT, newRT, jwtProvider.getProps().getExpire() / 1000);
    }

    @Override
    public void logout(Long userId, String refreshToken) {
        // 销毁当前设备的 Refresh Token
        if (StringUtils.hasText(refreshToken)) {
            try {
                Jwt jwt = jwtProvider.decode(refreshToken);
                String jti = jwtProvider.getJti(jwt);

                if (StringUtils.hasText(jti)) {
                    redisManager.delete(AuthConst.REFRESH_TOKEN_CACHE_PREFIX + userId + ":" + jti);
                }
            } catch (Exception e) {
                // 忽略解析异常：如果 RT 已经过期或非法，直接走下面的清除权限缓存逻辑即可
                log.warn("登出时解析 RT 失败 (可能已过期), userId={}", userId);
            }
        }
        // 清除该用户在 Redis 中的 LoginUser 权限缓存
        // 强制此 userId 相关的现有短效 AT 在下一次请求时重新查库验证状态，变相实现了全局退出
        authenticationService.evictLoginUserCache(userId);

        log.info("用户登出成功: userId={}", userId);
    }
}
