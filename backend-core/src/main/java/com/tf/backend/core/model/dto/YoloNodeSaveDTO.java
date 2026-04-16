package com.tf.backend.core.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record YoloNodeSaveDTO(

        @NotBlank(message = "节点名称不能为空")
        String nodeName,

        String description,

        @NotBlank(message = "主机地址不能为空")
        String host,

        @NotBlank(message = "端口号不能为空")
        @Pattern(regexp = "^\\d{1,5}$", message = "端口号格式不正确")
        String port,

        String httpProtocol,

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
