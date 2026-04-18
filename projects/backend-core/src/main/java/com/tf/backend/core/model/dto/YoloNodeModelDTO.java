package com.tf.backend.core.model.dto;

import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "YOLO节点模型与参数同步请求/响应DTO")
public record YoloNodeModelDTO(

        @Schema(description = "模型权重信息")
        YoloNodeWeightsDTO weights,

        @Schema(description = "当前推理参数字典")
        Map<String, Object> paramsMap
) {}
