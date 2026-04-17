package com.tf.backend.core.model.vo;

import com.tf.backend.core.common.enumeration.Status;
import com.tf.backend.core.model.entity.YoloNodeEntity;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "YOLO节点列表展示分页VO")
public record YoloNodeVO(

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
        LocalDateTime updatedAt
) {

    public static YoloNodeVO mapToVO(YoloNodeEntity entity) {
        return new YoloNodeVO(
                entity.getId(),
                entity.getNodeName(),
                entity.getDescription(),
                entity.getHost(),
                entity.getPort(),
                entity.getActiveWeightName(),
                entity.getStatus(),
                entity.getUpdatedAt()
        );
    }

}