package com.tf.backend.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "角色权限更新请求")
public record RolePermissionUpdateDTO(

        @Schema(description = "目标权限ID列表")
        @NotNull(message = "权限ID列表不能为空")
        List<Long> permissionIds
) {
}
