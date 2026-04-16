package com.tf.backend.core.application.infrastructure.repo.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tf.backend.core.application.infrastructure.repo.YoloWeightService;
import com.tf.backend.core.application.mapper.YoloWeightMapper;
import com.tf.backend.core.model.entity.YoloWeightEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class YoloWeightServiceImpl extends ServiceImpl<YoloWeightMapper, YoloWeightEntity> implements YoloWeightService {

    @Override
    public void removeByNodeId(Long nodeId) {
        this.remove(Wrappers.<YoloWeightEntity>lambdaQuery().eq(YoloWeightEntity::getNodeId, nodeId));
    }

    @Override
    public List<String> listAvailableWeights(Long nodeId) {
        return this.list(
                new LambdaQueryWrapper<YoloWeightEntity>().eq(YoloWeightEntity::getNodeId, nodeId)
        ).stream()
                .map(YoloWeightEntity::getFilename)
                .collect(Collectors.toList());
    }
}
