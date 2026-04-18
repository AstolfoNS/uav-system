package com.tf.backend.core.application.infrastructure.repo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tf.backend.core.model.dto.YoloNodeSaveDTO;
import com.tf.backend.core.model.entity.YoloNodeEntity;
import com.tf.backend.core.model.vo.YoloNodeDetailVO;
import com.tf.backend.core.model.vo.YoloNodeVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

public interface YoloNodeService extends IService<YoloNodeEntity> {

    IPage<YoloNodeVO> getPageList(Integer current, Integer size, String nodeName, Integer status);

    YoloNodeDetailVO getNodeDetail(Long id);

    @Transactional(rollbackFor = Exception.class)
    Long createNode(YoloNodeSaveDTO dto);

    @Transactional(rollbackFor = Exception.class)
    boolean updateNodeInfo(Long id, YoloNodeSaveDTO dto);

    @Transactional(rollbackFor = Exception.class)
    void deleteNodeWithCascades(Long id);

    @Transactional(rollbackFor = Exception.class)
    Map<String, Object> switchActiveTemplate(Long nodeId, String templateName);

    String getBaseUrlById(Long nodeId);

    void checkNodeNameUnique(Long id, String nodeName);
}
