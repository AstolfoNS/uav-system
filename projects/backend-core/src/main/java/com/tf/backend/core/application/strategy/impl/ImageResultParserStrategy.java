package com.tf.backend.core.application.strategy.impl;

import com.tf.backend.core.application.strategy.InferenceResultParserStrategy;
import com.tf.backend.core.common.enumeration.TaskType;
import com.tf.backend.core.model.entity.YoloDetectionRecordEntity;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.Map;

@Component
public class ImageResultParserStrategy implements InferenceResultParserStrategy {

    @Override
    public TaskType getSupportedType() {
        return TaskType.IMAGE;
    }

    @Override
    public void parseAndFill(YoloDetectionRecordEntity record, JsonNode dataNode) {
        if (dataNode.has("image_url")) {
            record.setResultUrl(dataNode.get("image_url").asString());
        }
        if (dataNode.has("count")) {
            record.setDetectCount(dataNode.get("count").asInt());
        }
        if (dataNode.has("detections")) {
            Map<String, Object> detailsMap = Map.of("detections", dataNode.get("detections"));
            record.setDetectionDetails(detailsMap);
        }
    }
}