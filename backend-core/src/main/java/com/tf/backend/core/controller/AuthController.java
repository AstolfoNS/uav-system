package com.tf.backend.core.controller;

import com.tf.backend.core.common.annotation.ExtractRefreshToken;
import com.tf.backend.core.common.enumeration.HttpCode;
import com.tf.backend.core.common.exception.BizException;
import com.tf.backend.core.common.exception.TokenAuthenticationException;
import com.tf.backend.core.common.response.R;
import com.tf.backend.core.application.domain.auth.AuthenticationService;
import com.tf.backend.core.application.domain.auth.LoginService;
import com.tf.backend.core.model.dto.LoginRequestDTO;
import com.tf.backend.core.model.dto.TokenResponseDTO;
import com.tf.backend.core.application.security.LoginUser;
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
    public R<TokenResponseDTO> login(@Validated @RequestBody LoginRequestDTO request) {
        TokenResponseDTO response = loginService.login(request);

        return R.ok(response, "登录成功");
    }

    @PostMapping("/refresh")
    public R<TokenResponseDTO> refresh(@ExtractRefreshToken String refreshToken) throws TokenAuthenticationException {
        if (refreshToken == null) {
            throw new BizException(HttpCode.BAD_REQUEST.getCode(), "缺失有效的 Refresh Token");
        }

        TokenResponseDTO response = authenticationService.refreshToken(refreshToken);

        return R.ok(response, "Token 刷新成功");
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
