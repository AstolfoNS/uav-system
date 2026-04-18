package com.tf.backend.core.model.dto;

import com.tf.backend.core.common.enumeration.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "用户个人资料修改请求对象")
public record UserProfileUpdateDTO(

        @Schema(description = "用户名", example = "uav_admin", minLength = 4, maxLength = 32)
        @NotBlank(message = "用户名不能为空")
        @Size(min = 4, max = 32, message = "用户名长度需在4到32个字符之间")
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名仅支持字母、数字和下划线")
        String username,

        @Schema(description = "用户昵称", example = "张三", maxLength = 64)
        @NotBlank(message = "昵称不能为空")
        @Size(max = 64, message = "昵称长度不能超过64个字符")
        String nickname,

        @Schema(description = "用户邮箱", example = "alice@example.com")
        @Email(message = "邮箱格式不正确")
        String email,

        @Schema(description = "用户手机号", example = "13800138000")
        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phoneNumber,

        @Schema(description = "用户头像地址", example = "http://example.com/avatar.jpg")
        String avatarUrl,

        @Schema(description = "用户性别：0=未知，1=男，2=女", example = "1", allowableValues = {"0", "1", "2"})
        Gender gender,

        @Schema(description = "用户简介", example = "这是一个热爱生活的人", maxLength = 500)
        @Size(max = 500, message = "个人简介过长")
        String introduction
) {
}
