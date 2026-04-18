package com.tf.backend.core.application.infrastructure.repo;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tf.backend.core.model.entity.YoloNodeParamEntity;

import java.util.List;
import java.util.Optional;

public interface YoloNodeParamService extends IService<YoloNodeParamEntity> {
    void removeByNodeId(Long nodeId);

    void removeByNodeIdAndTemplateName(Long nodeId, String templateName);

    List<YoloNodeParamEntity> listByNodeId(Long nodeId);

    Optional<YoloNodeParamEntity> getActiveOptByNodeId(Long nodeId);

    Optional<YoloNodeParamEntity> getOptByNodeIdAndTemplateName(Long nodeId, String templateName);

    void updateActiveByNodeId(Long nodeId, YoloNodeParamEntity updateEntity);
}
