package com.tf.backend.core.application.strategy.impl;

import com.tf.backend.core.application.strategy.InferenceResultParserStrategy;
import com.tf.backend.core.common.enumeration.TaskType;
import com.tf.backend.core.model.entity.YoloDetectionRecordEntity;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.Map;

@Component
public class VideoResultParserStrategy implements InferenceResultParserStrategy {

    @Override
    public TaskType getSupportedType() {
        return TaskType.VIDEO;
    }

    @Override
    public void parseAndFill(YoloDetectionRecordEntity record, JsonNode dataNode) {
        if (dataNode.has("video_url")) {
            record.setResultUrl(dataNode.get("video_url").asString());
        }
        if (dataNode.has("message")) {
            Map<String, Object> detailsMap = Map.of("message", dataNode.get("message").asString());
            record.setDetectionDetails(detailsMap);
        }
    }
}