package com.tf.backend.core.application.security;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Getter
public class AuthenticationToken extends AbstractAuthenticationToken {

    private final LoginUser loginUser;

    private final String token;


    public AuthenticationToken(LoginUser loginUser, String token) {
        super(buildAuthorities(loginUser));

        this.loginUser = loginUser;
        this.token = token;

        setAuthenticated(true);
    }

    private static List<GrantedAuthority> buildAuthorities(LoginUser user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        // 判断 user 是否为空
        if (user == null) {
            return authorities;
        }
        // 判断 user.getRoles() 是否为空
        if (!CollectionUtils.isEmpty(user.getRoles())) {
            user.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        }
        // 判断 user.getPermissions() 是否为空
        if (!CollectionUtils.isEmpty(user.getPermissions())) {
            user.getPermissions().forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        }
        return authorities;
    }

    @Override
    public @Nullable Object getCredentials() {
        return getToken();
    }

    @Override
    public @Nullable Object getPrincipal() {
        return getLoginUser();
    }
}

