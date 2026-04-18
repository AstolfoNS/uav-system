package com.tf.backend.core.controller;

import com.tf.backend.core.common.response.R;
import com.tf.backend.core.model.dto.UserPasswordUpdateDTO;
import com.tf.backend.core.model.dto.UserProfileUpdateDTO;
import com.tf.backend.core.application.infrastructure.repo.UserService;
import com.tf.backend.core.application.security.LoginUser;
import com.tf.backend.core.model.vo.UserProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/users")
@RestController
@Tag(name = "用户个人中心", description = "提供当前登录用户查询与修改个人资料的接口")
public class UserController {

    private final UserService userService;


    @GetMapping("/profile")
    @PreAuthorize("hasAuthority('user:profile:view')")
    @Operation(summary = "获取个人资料", description = "获取当前登录用户的完整个人信息")
    public R<UserProfileVO> getProfile(

            @Parameter(description = "自动注入参数: 当前登录的用户信息")
            @AuthenticationPrincipal
            LoginUser loginUser

    ) {
        UserProfileVO profileVO = userService.getCurrentUserProfile(
            loginUser.getId(),
            loginUser.getRoles(),
            loginUser.getPermissions()
        );

        return R.ok(profileVO);
    }


    @PutMapping("/profile")
    @PreAuthorize("hasAuthority('user:profile:update')")
    @Operation(summary = "修改个人资料", description = "支持修改用户名、昵称、邮箱、手机号、头像、性别和简介。修改用户名、邮箱或手机号时会进行唯一性校验。")
    public R<UserProfileVO> updateProfile(

            @Parameter(description = "自动注入参数: 当前登录的用户信息")
            @AuthenticationPrincipal
            LoginUser loginUser,

            @Valid
            @RequestBody
            UserProfileUpdateDTO dto

    ) {
        UserProfileVO profileVO = userService.updateCurrentUserProfile(
            loginUser.getId(),
            loginUser.getRoles(),
            loginUser.getPermissions(),
            dto
        );

        return R.ok(profileVO, "个人资料修改成功");
    }


    @PutMapping("/profile/password")
    @PreAuthorize("hasAuthority('user:profile:update')")
    @Operation(summary = "修改当前登录用户密码", description = "需提交当前密码与新密码，修改成功后即时生效")
    public R<Void> updatePassword(

            @Parameter(description = "自动注入参数: 当前登录的用户信息")
            @AuthenticationPrincipal
            LoginUser loginUser,

            @Valid
            @RequestBody
            UserPasswordUpdateDTO dto

    ) {
        userService.updateCurrentUserPassword(loginUser.getId(), dto);
        return R.okWithMsg("密码修改成功");
    }

}
