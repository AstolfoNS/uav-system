package com.tf.backend.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "RBAC角色管理视图")
public record AdminRbacRoleVO(

        @Schema(description = "角色ID")
        Long id,

        @Schema(description = "角色编码")
        String code,

        @Schema(description = "角色名称")
        String name,

        @Schema(description = "角色描述")
        String description,

        @Schema(description = "角色级别")
        Integer level,

        @Schema(description = "排序")
        Integer sortOrder,

        @Schema(description = "权限ID列表")
        List<Long> permissionIds,

        @Schema(description = "权限编码列表")
        List<String> permissions
) {
}
