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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
@Tag(name = "认证模块", description = "登录、登出及Token刷新接口")
public class AuthController {

    private final LoginService loginService;

    private final AuthenticationService authenticationService;


    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "根据用户名和密码获取Token")
    public R<TokenResponseDTO> login(

            @Parameter(description = "登录参数: username password rememberMe")
            @Validated
            @RequestBody
            LoginRequestDTO request

    ) {
        TokenResponseDTO response = loginService.login(request);

        return R.ok(response, "登录成功");
    }


    @PostMapping("/refresh")
    @Operation(summary = "刷新Token", description = "使用Refresh Token换取新的Access Token")
    public R<TokenResponseDTO> refresh(

            @Parameter(description = "需要刷新的 Refresh-Token")
            @ExtractRefreshToken
            String refreshToken

    ) throws TokenAuthenticationException {
        if (refreshToken == null) {
            throw new BizException(HttpCode.BAD_REQUEST.getCode(), "缺失有效的 Refresh Token");
        }

        TokenResponseDTO response = authenticationService.refreshToken(refreshToken);

        return R.ok(response, "Token 刷新成功");
    }


    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "注销当前登录状态")
    public R<Void> logout(

            @Parameter(description = "自动注入参数: 当前登录的用户信息")
            @AuthenticationPrincipal
            LoginUser loginUser,

            @Parameter(description = "需要销毁的 Refresh-Token")
            @ExtractRefreshToken(required = false)
            String refreshToken

    ) throws TokenAuthenticationException {
        if (loginUser == null) {
            throw new TokenAuthenticationException("未登录或会话已过期");
        }

        loginService.logout(loginUser.getId(), refreshToken);

        return R.okWithMsg("您已成功退出登录");
    }


}
