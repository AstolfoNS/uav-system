package com.tf.backend.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "YOLO节点参数模板展示VO")
public record YoloNodeParamVO(

        @Schema(description = "模板记录ID")
        Long id,

        @Schema(description = "所属节点ID")
        Long nodeId,

        @Schema(description = "模板名称")
        String templateName,

        @Schema(description = "模板描述")
        String description,

        @Schema(description = "是否当前激活")
        Boolean isActive,

        @Schema(description = "参数字典")
        Map<String, Object> params,

        @Schema(description = "最后更新时间")
        LocalDateTime updatedAt
) {}