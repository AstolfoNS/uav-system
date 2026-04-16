package com.tf.backend.core.application.domain.yolo;

import com.tf.backend.core.model.entity.YoloDetectionRecordEntity;
import org.springframework.web.multipart.MultipartFile;

/**
 * YOLO 核心推理业务服务
 */
public interface YoloInferenceService {

    /**
     * 执行单张图像的预测检测
     *
     * @param nodeId 物理机节点ID
     * @param file   图像文件
     * @return 包含预测结果的完整数据库记录
     */
    YoloDetectionRecordEntity predictImage(Long nodeId, MultipartFile file);

    /**
     * 执行视频片段的预测检测
     *
     * @param nodeId 物理机节点ID
     * @param file   视频文件
     * @return 包含预测结果的完整数据库记录
     */
    YoloDetectionRecordEntity predictVideo(Long nodeId, MultipartFile file);
}