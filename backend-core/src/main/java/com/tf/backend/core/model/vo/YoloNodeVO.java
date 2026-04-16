package com.tf.backend.core.model.vo;

import com.tf.backend.core.common.enumeration.Status;
import com.tf.backend.core.model.entity.YoloNodeEntity;

import java.time.LocalDateTime;

public record YoloNodeVO(

        Long id,

        String nodeName,

        String description,

        String host,

        String port,

        String activeWeightName,

        Status status,

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