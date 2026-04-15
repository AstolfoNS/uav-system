package com.tf.backend.core.common.util;

import com.tf.backend.core.security.AuthenticationToken;
import com.tf.backend.core.security.LoginUser;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * 获取当前登录认证信息
     */
    @Nullable
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 是否已登录（排除匿名用户）
     */
    public static boolean isAuthenticated() {
        Authentication auth = getAuthentication();

        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    /**
     * 获取当前登录用户（LoginUser）
     */
    public static Optional<LoginUser> getLoginUser() {
        Authentication auth = getAuthentication();

        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof LoginUser loginUser) {
            return Optional.of(loginUser);
        }

        if (auth instanceof AuthenticationToken token) {
            Object user = token.getPrincipal();

            if (user instanceof LoginUser loginUser) {
                return Optional.of(loginUser);
            }
        }

        return Optional.empty();
    }

    /**
     * 获取当前用户 ID
     */
    @Nullable
    public static Long getUserId() {
        return getLoginUser().map(LoginUser::getId).orElse(null);
    }

    /**
     * 获取当前用户名
     */
    @Nullable
    public static String getUsername() {
        return getLoginUser().map(LoginUser::getUsername).orElse(null);
    }

    /**
     * 获取当前 Token
     */
    @Nullable
    public static String getToken() {
        Authentication auth = getAuthentication();

        if (auth instanceof AuthenticationToken token) {
            return (String) token.getCredentials();
        }
        return null;
    }

    /**
     * 获取当前用户角色列表（不可修改）
     */
    public static List<String> getRoles() {
        return getLoginUser()
                .map(LoginUser::getRoles)
                .map(Collections::unmodifiableList)
                .orElse(Collections.emptyList());
    }

    /**
     * 获取当前用户权限列表（GrantedAuthority）
     */
    public static Collection<? extends GrantedAuthority> getAuthorities() {
        Authentication auth = getAuthentication();

        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            return Collections.emptyList();
        }
        return auth.getAuthorities();
    }

    /**
     * 获取权限字符串 Set
     */
    public static Set<String> getAuthoritySet() {
        return getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    /**
     * 是否拥有指定角色（自动补全 ROLE_ 前缀）
     */
    public static boolean hasRole(String roleCode) {
        if (roleCode == null) {
            return false;
        }
        return getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch((roleCode.startsWith("ROLE_") ? roleCode : "ROLE_" + roleCode)::equals);
    }

    /**
     * 是否拥有指定权限
     */
    public static boolean hasPermission(String permissionCode) {
        if (permissionCode == null) {
            return false;
        }
        return getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(permissionCode::equals);
    }
}
