package com.tf.backend.core.application.domain.yolo.impl;

import com.tf.backend.core.application.domain.yolo.YoloNodeClientService;
import com.tf.backend.core.application.domain.yolo.YoloNodeManageService;
import com.tf.backend.core.application.domain.yolo.YoloNodeSyncService;
import com.tf.backend.core.application.infrastructure.repo.YoloNodeParamService;
import com.tf.backend.core.application.infrastructure.repo.YoloNodeService;
import com.tf.backend.core.application.infrastructure.repo.YoloWeightService;
import com.tf.backend.core.common.exception.BizException;
import com.tf.backend.core.model.dto.YoloNodeParamCreateDTO;
import com.tf.backend.core.model.dto.YoloNodeParamUpdateDTO;
import com.tf.backend.core.model.dto.YoloNodeSaveDTO;
import com.tf.backend.core.model.dto.YoloNodeWeightsDTO;
import com.tf.backend.core.model.entity.YoloNodeEntity;
import com.tf.backend.core.model.entity.YoloNodeParamEntity;
import com.tf.backend.core.model.vo.YoloNodeParamVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoloNodeManageServiceImpl implements YoloNodeManageService {

    private final YoloNodeClientService yoloNodeClientService;

    private final YoloNodeService yoloNodeService;

    private final YoloNodeParamService yoloNodeParamService;

    private final YoloWeightService yoloWeightService;

    private final YoloNodeSyncService yoloNodeSyncService;


    @Override
    public void addNode(YoloNodeSaveDTO dto) {
        // 调用基础服务落库，获取新节点 ID
        Long newNodeId = yoloNodeService.createNode(dto);

        // 触发异步网络同步
        triggerAsyncSync(newNodeId);
    }

    @Override
    public void updateNode(Long id, YoloNodeSaveDTO dto) {
        // 调用基础服务更新数据库，并返回“URL是否发生了改变”
        boolean urlChanged = yoloNodeService.updateNodeInfo(id, dto);

        // 如果连接地址变了，触发异步网络同步
        if (urlChanged) {
            triggerAsyncSync(id);
        }
    }

    /**
     * 切换节点当前生效的参数模板
     */
    @Override
    public void applyParamTemplate(Long nodeId, String templateName) {
        // 调用 YoloNodeService: 开启事务，把旧模板 is_active 设为 0，目标模板设为 1，并返回该模板的 params JSON
        Map<String, Object> targetParams = yoloNodeService.switchActiveTemplate(nodeId, templateName);

        // 调用 YoloNodeClientService: 把取出来的 JSON 推送给 FastAPI 物理机
        yoloNodeClientService.updateParamsBatch(yoloNodeService.getBaseUrlById(nodeId), targetParams);

        // 调用 YoloNodeSyncService 再次对齐验证
        triggerAsyncSync(nodeId);
    }

    @Override
    public List<YoloNodeParamVO> listParamTemplates(Long nodeId) {
        ensureNodeExists(nodeId);
        return yoloNodeParamService.listByNodeId(nodeId).stream().map(this::toParamVO).collect(Collectors.toList());
    }

    @Override
    public YoloNodeParamVO getParamTemplate(Long nodeId, String templateName) {
        ensureNodeExists(nodeId);
        YoloNodeParamEntity entity = yoloNodeParamService.getOptByNodeIdAndTemplateName(nodeId, templateName)
                .orElseThrow(() -> new BizException("找不到目标参数模板: " + templateName));
        return toParamVO(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createParamTemplate(Long nodeId, YoloNodeParamCreateDTO dto) {
        ensureNodeExists(nodeId);
        assertTemplateNotExists(nodeId, dto.templateName());

        if (Boolean.TRUE.equals(dto.isActive())) {
            deactivateAllTemplates(nodeId);
        }

        YoloNodeParamEntity entity = YoloNodeParamEntity.builder()
                .nodeId(nodeId)
                .templateName(dto.templateName())
                .description(dto.description())
                .params(dto.params())
                .isActive(Boolean.TRUE.equals(dto.isActive()))
                .build();

        yoloNodeParamService.save(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateParamTemplate(Long nodeId, String templateName, YoloNodeParamUpdateDTO dto) {
        ensureNodeExists(nodeId);
        YoloNodeParamEntity entity = yoloNodeParamService.getOptByNodeIdAndTemplateName(nodeId, templateName)
                .orElseThrow(() -> new BizException("找不到目标参数模板: " + templateName));

        if (dto.description() != null) {
            entity.setDescription(dto.description());
        }
        if (dto.params() != null) {
            entity.setParams(dto.params());
        }
        if (dto.isActive() != null) {
            if (Boolean.TRUE.equals(dto.isActive())) {
                deactivateAllTemplates(nodeId);
            }
            entity.setIsActive(dto.isActive());
        }

        yoloNodeParamService.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteParamTemplate(Long nodeId, String templateName) {
        ensureNodeExists(nodeId);
        YoloNodeParamEntity entity = yoloNodeParamService.getOptByNodeIdAndTemplateName(nodeId, templateName)
                .orElseThrow(() -> new BizException("找不到目标参数模板: " + templateName));

        boolean wasActive = Boolean.TRUE.equals(entity.getIsActive());
        yoloNodeParamService.removeById(entity.getId());

        if (wasActive) {
            yoloNodeParamService.listByNodeId(nodeId).stream().findFirst().ifPresent(this::activateTemplate);
        }
    }

    @Override
    public YoloNodeWeightsDTO getWeightSummary(Long nodeId) {
        ensureNodeExists(nodeId);
        return yoloWeightService.getWeightSummary(nodeId);
    }

    /**
     * 异步触发节点同步（不阻塞主业务流程）
     */
    private void triggerAsyncSync(Long nodeId) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Triggering background sync for updated/created node ID: {}", nodeId);

                yoloNodeSyncService.syncNodeState(nodeId);
            } catch (Exception e) {
                log.error("Background sync trigger failed for node ID: {}", nodeId, e);
            }
        });
    }

    private void ensureNodeExists(Long nodeId) {
        YoloNodeEntity node = yoloNodeService.getById(nodeId);
        if (node == null) {
            throw new BizException("目标 YOLO 节点不存在");
        }
    }

    private void assertTemplateNotExists(Long nodeId, String templateName) {
        if (yoloNodeParamService.getOptByNodeIdAndTemplateName(nodeId, templateName).isPresent()) {
            throw new BizException("参数模板名称已存在，请更换");
        }
    }

    private void deactivateAllTemplates(Long nodeId) {
        YoloNodeParamEntity updateEntity = new YoloNodeParamEntity();
        updateEntity.setIsActive(false);
        yoloNodeParamService.updateActiveByNodeId(nodeId, updateEntity);
    }

    private void activateTemplate(YoloNodeParamEntity entity) {
        deactivateAllTemplates(entity.getNodeId());
        entity.setIsActive(true);
        yoloNodeParamService.updateById(entity);
    }

    private YoloNodeParamVO toParamVO(YoloNodeParamEntity entity) {
        return new YoloNodeParamVO(
                entity.getId(),
                entity.getNodeId(),
                entity.getTemplateName(),
                entity.getDescription(),
                entity.getIsActive(),
                entity.getParams(),
                entity.getUpdatedAt()
        );
    }
}
