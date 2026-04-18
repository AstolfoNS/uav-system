package com.tf.backend.core.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "YOLO节点保存请求参数")
public record YoloNodeSaveDTO(

        @Schema(description = "节点名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "节点甲")
        @NotBlank(message = "节点名称不能为空")
        String nodeName,

        @Schema(description = "节点描述", example = "位于大门处的检测节点")
        String description,

        @Schema(description = "主机地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "192.168.1.100")
        @NotBlank(message = "主机地址不能为空")
        String host,

        @Schema(description = "端口号", requiredMode = Schema.RequiredMode.REQUIRED, example = "8000")
        @NotBlank(message = "端口号不能为空")
        @Pattern(regexp = "^\\d{1,5}$", message = "端口号格式不正确")
        String port,

        @Schema(description = "HTTP协议", example = "http")
        String httpProtocol,

        @Schema(description = "API版本", example = "v1")
        String apiVersion
) {
    public YoloNodeSaveDTO {
        if (httpProtocol == null || httpProtocol.isBlank()) {
            httpProtocol = "http";
        }
        if (apiVersion == null || apiVersion.isBlank()) {
            apiVersion = "v1";
        }
    }
}
