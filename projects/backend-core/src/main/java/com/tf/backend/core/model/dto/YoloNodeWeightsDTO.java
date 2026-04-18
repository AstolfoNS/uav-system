package com.tf.backend.core.model.dto;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "YOLO节点模型权重信息DTO")
public record YoloNodeWeightsDTO(

        @Schema(description = "当前处于激活状态的权重名称")
        String activeWeight,

        @Schema(description = "物理机上所有可用的权重列表")
        List<String> availableWeights
) {}
