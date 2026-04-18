package com.tf.backend.core.filter;

import com.tf.backend.core.common.exception.TokenAuthenticationException;
import com.tf.backend.core.application.domain.auth.AuthenticationService;
import com.tf.backend.core.application.infrastructure.cache.RedisManager;
import com.tf.backend.core.application.security.LoginUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER_START = "Bearer ";

    private final AuthenticationService authenticationService;

    private final RedisManager redisManager;


    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = extractToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            Authentication authentication = authenticationService.buildAuthenticationFor(token);

            if (authentication != null) {
                SecurityContextHolder.getContext().setAuthentication(authentication);

                if (authentication.getPrincipal() instanceof LoginUser loginUser) {
                    redisManager.hPut(
                            "sys:user_active_time",
                            loginUser.getId().toString(),
                            String.valueOf(System.currentTimeMillis())
                    );
                }
            }
        } catch (TokenAuthenticationException | JwtException e) {
            // 捕获预期的认证失败异常（如 Token 过期、无效、被篡改）
            log.warn("JWT 认证失败, IP: {}, 原因: {}", request.getRemoteAddr(), e.getMessage());

            SecurityContextHolder.clearContext();
            // 返回标准的 401 状态码，前端拦截器可以据此重定向到登录页
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "无效或已过期的 Token");
            return;
        } catch (Exception e) {
            // 捕获真正的系统级异常（如 Redis 宕机、数据库异常等）
            log.error("构建 Authentication 发生系统异常：{}", e.getMessage(), e);

            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "服务器内部异常");

            return;
        }

        filterChain.doFilter(request, response);
    }

    @Nullable
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith(AUTH_HEADER_START)) {
            return authHeader.substring(AUTH_HEADER_START.length());
        }

        return null;
    }
}
