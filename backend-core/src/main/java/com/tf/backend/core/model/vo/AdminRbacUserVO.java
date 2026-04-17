package com.tf.backend.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "RBAC用户管理视图")
public record AdminRbacUserVO(

        @Schema(description = "用户ID")
        Long id,

        @Schema(description = "用户名")
        String username,

        @Schema(description = "昵称")
        String nickname,

        @Schema(description = "角色ID列表")
        List<Long> roleIds,

        @Schema(description = "角色编码列表")
        List<String> roles
) {
}
