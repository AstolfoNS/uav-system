package com.tf.backend.core.application.domain.yolo;

import com.tf.backend.core.model.dto.YoloNodeSaveDTO;
import com.tf.backend.core.model.dto.YoloNodeParamCreateDTO;
import com.tf.backend.core.model.dto.YoloNodeParamUpdateDTO;
import com.tf.backend.core.model.dto.YoloNodeWeightsDTO;
import com.tf.backend.core.model.vo.YoloNodeParamVO;

import java.util.List;

/**
 * YOLO 节点操作聚合服务
 */
public interface YoloNodeManageService {

    void addNode(YoloNodeSaveDTO dto);

    void updateNode(Long id, YoloNodeSaveDTO dto);

    void applyParamTemplate(Long nodeId, String templateName);

    List<YoloNodeParamVO> listParamTemplates(Long nodeId);

    YoloNodeParamVO getParamTemplate(Long nodeId, String templateName);

    void createParamTemplate(Long nodeId, YoloNodeParamCreateDTO dto);

    void updateParamTemplate(Long nodeId, String templateName, YoloNodeParamUpdateDTO dto);

    void deleteParamTemplate(Long nodeId, String templateName);

    YoloNodeWeightsDTO getWeightSummary(Long nodeId);
}
