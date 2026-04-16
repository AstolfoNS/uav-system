package com.tf.backend.core.application.strategy;

import com.tf.backend.core.common.enumeration.TaskType;
import com.tf.backend.core.model.entity.YoloDetectionRecordEntity;
import tools.jackson.databind.JsonNode;

/**
 * 预测结果解析策略接口
 */
public interface InferenceResultParserStrategy {

    /**
     * 声明该策略支持的任务类型
     */
    TaskType getSupportedType();

    /**
     * 执行具体的解析与填充逻辑
     */
    void parseAndFill(YoloDetectionRecordEntity record, JsonNode dataNode);
}