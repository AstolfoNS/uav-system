package com.tf.backend.core.application.infrastructure.repo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tf.backend.core.model.entity.YoloDetectionRecordEntity;

/**
 * YOLO目标检测历史记录 基础数据服务接口
 */
public interface YoloDetectionRecordService extends IService<YoloDetectionRecordEntity> {

    IPage<YoloDetectionRecordEntity> getRecordPage(Integer current, Integer size, Long nodeId, Integer taskType, String originalFilename);

}