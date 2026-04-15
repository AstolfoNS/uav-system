package com.tf.backend.core.model.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema; // 如果你用了 Knife4j/Swagger 可以加上

@Schema(description = "用户登录请求参数")
public record LoginRequest(

        @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin")
        @NotBlank(message = "用户名不能为空")
        String username,

        @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
        @NotBlank(message = "密码不能为空")
        String password,

        @Schema(description = "是否记住我（决定 Refresh Token 的寿命）", example = "true")
        // 注意这里用的是基本类型 boolean，如果前端不传这个字段，它会默认取值为 false
        boolean rememberMe
) {
}
