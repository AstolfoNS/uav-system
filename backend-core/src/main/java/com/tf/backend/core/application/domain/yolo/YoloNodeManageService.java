package com.tf.backend.core.application.domain.yolo;

import com.tf.backend.core.model.dto.YoloNodeSaveDTO;

/**
 * YOLO 节点操作聚合服务
 */
public interface YoloNodeManageService {

    void addNode(YoloNodeSaveDTO dto);

    void updateNode(Long id, YoloNodeSaveDTO dto);

    void applyParamTemplate(Long nodeId, String templateName);
}
