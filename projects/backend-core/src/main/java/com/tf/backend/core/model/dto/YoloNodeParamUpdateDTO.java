package com.tf.backend.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "YOLO节点参数模板更新请求")
public record YoloNodeParamUpdateDTO(

        @Schema(description = "模板描述", example = "默认检测参数")
        String description,

        @Schema(description = "参数字典")
        Map<String, Object> params,

        @Schema(description = "是否设为当前激活模板", example = "false")
        Boolean isActive
) {}