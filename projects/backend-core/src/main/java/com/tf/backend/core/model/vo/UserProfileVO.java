package com.tf.backend.core.model.vo;

import com.tf.backend.core.common.enumeration.Gender;
import com.tf.backend.core.model.entity.UserEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "当前登录用户个人资料响应VO")
public record UserProfileVO(

    @Schema(description = "用户ID", example = "1", minimum = "1")
        Long id,

    @Schema(description = "用户名", example = "admin", maxLength = 64)
        String username,

    @Schema(description = "昵称", example = "系统管理员", minLength = 1, maxLength = 64)
        String nickname,

    @Schema(description = "头像地址", example = "https://cdn.example.com/avatar/admin.png", format = "uri")
        String avatarUrl,

    @Schema(description = "邮箱", example = "admin@example.com", format = "email", maxLength = 255)
        String email,

    @Schema(description = "手机号", example = "13800138000", pattern = "^$|^1[3-9]\\d{9}$")
        String phoneNumber,

    @Schema(description = "性别：0=未知，1=男，2=女", example = "1", allowableValues = {"0", "1", "2"})
        Gender gender,

    @Schema(description = "个人简介", example = "负责系统管理与运维", maxLength = 500)
        String introduction,

    @Schema(description = "最近登录时间", example = "2026-04-17T10:20:30")
        LocalDateTime lastLoginTime,

    @Schema(description = "角色编码列表", example = "[\"ADMIN\",\"OPERATOR\"]")
        List<String> roles,

    @Schema(description = "权限编码列表", example = "[\"user:profile:read\",\"user:profile:update\"]")
        List<String> permissions
) {

    public static UserProfileVO mapToVO(UserEntity user, List<String> roles, List<String> permissions) {
        return of(user, roles, permissions);
    }


    public static UserProfileVO of(UserEntity user, List<String> roles, List<String> permissions) {
        return new UserProfileVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getGender(),
                user.getIntroduction(),
                user.getLastLoginTime(),
                roles,
                permissions
        );
    }

}