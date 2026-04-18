package com.tf.backend.core.application.infrastructure.repo.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tf.backend.core.application.infrastructure.repo.YoloNodeParamService;
import com.tf.backend.core.application.mapper.YoloNodeParamMapper;
import com.tf.backend.core.model.entity.YoloNodeParamEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class YoloNodeParamServiceImpl extends ServiceImpl<YoloNodeParamMapper, YoloNodeParamEntity> implements YoloNodeParamService {

    @Override
    public void removeByNodeId(Long nodeId) {
        this.remove(Wrappers.<YoloNodeParamEntity>lambdaQuery().eq(YoloNodeParamEntity::getNodeId, nodeId));
    }

    @Override
    public void removeByNodeIdAndTemplateName(Long nodeId, String templateName) {
        this.remove(
                Wrappers.<YoloNodeParamEntity>lambdaQuery()
                        .eq(YoloNodeParamEntity::getNodeId, nodeId)
                        .eq(YoloNodeParamEntity::getTemplateName, templateName)
        );
    }

    @Override
    public List<YoloNodeParamEntity> listByNodeId(Long nodeId) {
        return this.list(
                Wrappers.<YoloNodeParamEntity>lambdaQuery()
                        .eq(YoloNodeParamEntity::getNodeId, nodeId)
                        .orderByDesc(YoloNodeParamEntity::getIsActive)
                        .orderByDesc(YoloNodeParamEntity::getUpdatedAt)
        );
    }

    @Override
    public Optional<YoloNodeParamEntity> getActiveOptByNodeId(Long nodeId) {
        return this.getOneOpt(
                Wrappers.<YoloNodeParamEntity>lambdaQuery()
                        .eq(YoloNodeParamEntity::getNodeId, nodeId)
                        .eq(YoloNodeParamEntity::getIsActive, true)
        );
    }

    @Override
    public Optional<YoloNodeParamEntity> getOptByNodeIdAndTemplateName(Long nodeId, String templateName) {
        return this.getOneOpt(
                Wrappers.<YoloNodeParamEntity>lambdaQuery()
                        .eq(YoloNodeParamEntity::getNodeId, nodeId)
                        .eq(YoloNodeParamEntity::getTemplateName, templateName)
        );
    }

    @Override
    public void updateActiveByNodeId(Long nodeId, YoloNodeParamEntity updateEntity) {
        this.update(
                updateEntity,
                new LambdaQueryWrapper<YoloNodeParamEntity>()
                        .eq(YoloNodeParamEntity::getNodeId, nodeId)
                        .eq(YoloNodeParamEntity::getIsActive, true)
        );
    }

}
