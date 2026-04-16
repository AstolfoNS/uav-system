package com.tf.backend.core.application.infrastructure.repo;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tf.backend.core.model.entity.YoloWeightEntity;

import java.util.List;

public interface YoloWeightService extends IService<YoloWeightEntity> {

    void removeByNodeId(Long nodeId);

    List<String> listAvailableWeights(Long nodeId);
}
