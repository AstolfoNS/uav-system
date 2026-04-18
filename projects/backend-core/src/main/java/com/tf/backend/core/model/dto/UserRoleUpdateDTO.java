package com.tf.backend.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "用户角色更新请求")
public record UserRoleUpdateDTO(

        @Schema(description = "目标角色ID列表")
        @NotNull(message = "角色ID列表不能为空")
        List<Long> roleIds
) {
}
