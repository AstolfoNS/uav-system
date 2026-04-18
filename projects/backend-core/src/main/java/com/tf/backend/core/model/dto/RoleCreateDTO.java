package com.tf.backend.core.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RoleCreateDTO(

        @NotBlank(message = "角色编码不能为空")
        @Size(max = 64, message = "角色编码长度不能超过64个字符")
        @Pattern(
                regexp = "^[a-z0-9]+(?::[a-z0-9]+)*$",
                message = "角色编码仅支持小写字母、数字、冒号，且不能以冒号开头或结尾"
        )
        String code,

        @NotBlank(message = "角色名称不能为空")
        @Size(max = 64, message = "角色名称长度不能超过64个字符")
        String name,

        @Size(max = 512, message = "角色描述长度不能超过512个字符")
        String description,

        @Min(value = 0, message = "角色等级不能小于0")
        Integer level,

        @Min(value = 0, message = "排序顺序不能小于0")
        Integer sortOrder,

        List<Long> permissionIds

) {
}
