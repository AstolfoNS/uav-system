package com.tf.backend.core.application.infrastructure.repo.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tf.backend.core.application.infrastructure.repo.YoloDetectionRecordService;
import com.tf.backend.core.application.mapper.YoloDetectionRecordMapper;
import com.tf.backend.core.common.enumeration.TaskType;
import com.tf.backend.core.model.entity.YoloDetectionRecordEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * YOLO目标检测历史记录 基础数据服务实现类
 */
@Service
public class YoloDetectionRecordServiceImpl extends ServiceImpl<YoloDetectionRecordMapper, YoloDetectionRecordEntity> implements YoloDetectionRecordService {

    @Override
    public IPage<YoloDetectionRecordEntity> getRecordPage(
            Integer current,
            Integer size,
            Long nodeId,
            Integer taskType,
            String originalFilename
    ) {
        IPage<YoloDetectionRecordEntity> page = new Page<>(current, size);

        TaskType taskTypeFilter = null;
        if (taskType != null) {
            taskTypeFilter = TaskType.fromCode(taskType);
            // 非法任务类型直接返回空页，避免错误条件导致全量返回。
            if (taskTypeFilter == null) {
                return page;
            }
        }

        LambdaQueryWrapper<YoloDetectionRecordEntity> wrapper = Wrappers.<YoloDetectionRecordEntity>lambdaQuery()
                .eq(nodeId != null, YoloDetectionRecordEntity::getNodeId, nodeId)
                .eq(taskTypeFilter != null, YoloDetectionRecordEntity::getTaskType, taskTypeFilter)
                .like(StringUtils.hasText(originalFilename), YoloDetectionRecordEntity::getOriginalFilename, originalFilename)
                .orderByDesc(YoloDetectionRecordEntity::getCreatedAt);

        return this.page(page, wrapper);
    }
}