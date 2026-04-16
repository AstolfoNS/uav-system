package com.tf.backend.core.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tf.backend.core.model.entity.YoloDetectionRecordEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * YOLO目标检测历史记录 Mapper 接口
 */
@Mapper
public interface YoloDetectionRecordMapper extends BaseMapper<YoloDetectionRecordEntity> {
}