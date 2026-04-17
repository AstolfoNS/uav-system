package com.tf.backend.core.controller;

import com.tf.backend.core.common.response.R;
import com.tf.backend.core.common.util.SecurityUtils;
import com.tf.backend.core.model.dto.UserProfileUpdateDTO;
import com.tf.backend.core.application.infrastructure.repo.UserService;
import com.tf.backend.core.application.security.LoginUser;
import com.tf.backend.core.model.vo.UserProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/users")
@RestController
@Tag(name = "用户个人中心", description = "提供当前登录用户查询与修改个人资料的接口")
public class UserController {

    private final UserService userService;


    @GetMapping("/profile")
    @Operation(summary = "获取个人资料", description = "获取当前登录用户的完整个人信息")
    public R<UserProfileVO> getProfile() {
        LoginUser loginUser = SecurityUtils.requireLoginUser();

        UserProfileVO profileVO = userService.getCurrentUserProfile(
            loginUser.getId(),
            loginUser.getRoles(),
            loginUser.getPermissions()
        );

        return R.ok(profileVO);
    }


    @PutMapping("/profile")
    @Operation(summary = "修改个人资料", description = "支持修改昵称、邮箱、手机号、头像、性别和简介。修改邮箱或手机号时会进行唯一性校验。")
    public R<UserProfileVO> updateProfile(

            @Valid
            @RequestBody
            UserProfileUpdateDTO dto

    ) {
        LoginUser loginUser = SecurityUtils.requireLoginUser();

        UserProfileVO profileVO = userService.updateCurrentUserProfile(
            loginUser.getId(),
            loginUser.getRoles(),
            loginUser.getPermissions(),
            dto
        );

        return R.ok(profileVO, "个人资料修改成功");
    }

}
