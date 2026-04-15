package com.tf.backend.core.controller;

import com.tf.backend.core.common.annotation.ExtractRefreshToken;
import com.tf.backend.core.common.enumeration.HttpCode;
import com.tf.backend.core.common.exception.BizException;
import com.tf.backend.core.common.exception.TokenAuthenticationException;
import com.tf.backend.core.common.response.R;
import com.tf.backend.core.domain.auth.AuthenticationService;
import com.tf.backend.core.domain.auth.LoginService;
import com.tf.backend.core.model.dto.LoginRequest;
import com.tf.backend.core.model.dto.TokenResponse;
import com.tf.backend.core.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final LoginService loginService;

    private final AuthenticationService authenticationService;


    @PostMapping("/login")
    public R<TokenResponse> login(@Validated @RequestBody LoginRequest request) {
        return R.ok("登录成功", loginService.login(request));
    }

    @PostMapping("/refresh")
    public R<TokenResponse> refresh(@ExtractRefreshToken String refreshToken) throws TokenAuthenticationException {
        if (refreshToken == null) {
            throw new BizException(HttpCode.BAD_REQUEST.getCode(), "缺失有效的 Refresh Token");
        }
        return R.ok("Token 刷新成功", authenticationService.refreshToken(refreshToken));
    }

    @PostMapping("/logout")
    public R<Void> logout(
            @AuthenticationPrincipal LoginUser loginUser,
            @ExtractRefreshToken(required = false) String refreshToken
    ) throws TokenAuthenticationException {
        if (loginUser == null) {
            throw new TokenAuthenticationException("未登录或会话已过期");
        }
        loginService.logout(loginUser.getId(), refreshToken);

        return R.okWithMsg("您已成功退出登录");
    }

}
