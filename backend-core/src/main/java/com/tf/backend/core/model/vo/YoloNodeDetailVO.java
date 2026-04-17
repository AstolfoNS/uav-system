package com.tf.backend.core.model.vo;

import com.tf.backend.core.common.enumeration.Status;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "YOLO节点详细信息展示VO")
public record YoloNodeDetailVO(

        @Schema(description = "节点主键ID")
        Long id,

        @Schema(description = "节点名称")
        String nodeName,

        @Schema(description = "节点描述")
        String description,

        @Schema(description = "主机地址")
        String host,

        @Schema(description = "端口号")
        String port,

        @Schema(description = "当前激活的模型权重")
        String activeWeightName,

        @Schema(description = "节点状态")
        Status status,

        @Schema(description = "最后更新时间")
        LocalDateTime updatedAt,
        
        @Schema(description = "物理机上的可用权重列表")
        List<String> availableWeights,

        @Schema(description = "当前的推理参数")
        Map<String, Object> currentParams
) {}