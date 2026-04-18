package com.tf.backend.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "RBAC权限管理视图")
public record AdminRbacPermissionVO(

        @Schema(description = "权限ID")
        Long id,

        @Schema(description = "权限编码")
        String code,

        @Schema(description = "权限名称")
        String name,

        @Schema(description = "权限类型编码")
        Integer type,

        @Schema(description = "权限描述")
        String description,

        @Schema(description = "排序")
        Integer sortOrder
) {
}
