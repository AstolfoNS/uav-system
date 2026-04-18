package com.tf.backend.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Schema(description = "YOLO节点参数模板创建请求")
public record YoloNodeParamCreateDTO(

        @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "default")
        @NotBlank(message = "模板名称不能为空")
        String templateName,

        @Schema(description = "模板描述", example = "默认检测参数")
        String description,

        @Schema(description = "参数字典", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "参数字典不能为空")
        Map<String, Object> params,

        @Schema(description = "是否设为当前激活模板", example = "false")
        Boolean isActive
) {}