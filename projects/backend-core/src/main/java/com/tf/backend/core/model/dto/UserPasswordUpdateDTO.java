package com.tf.backend.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "用户修改密码请求对象")
public record UserPasswordUpdateDTO(

        @Schema(description = "当前密码", example = "old_password")
        @NotBlank(message = "当前密码不能为空")
        String currentPassword,

        @Schema(description = "新密码", example = "new_password", minLength = 6, maxLength = 64)
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 64, message = "新密码长度需在6到64个字符之间")
        String newPassword,

        @Schema(description = "确认新密码", example = "new_password")
        @NotBlank(message = "确认密码不能为空")
        String confirmPassword
) {
}
