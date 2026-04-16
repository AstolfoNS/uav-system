package com.tf.backend.core.application.domain.yolo.impl;

import com.tf.backend.core.application.domain.yolo.YoloNodeClientService;
import com.tf.backend.core.application.domain.yolo.YoloNodeManageService;
import com.tf.backend.core.application.domain.yolo.YoloNodeSyncService;
import com.tf.backend.core.application.infrastructure.repo.YoloNodeService;
import com.tf.backend.core.model.dto.YoloNodeSaveDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoloNodeManageServiceImpl implements YoloNodeManageService {

    private final YoloNodeClientService yoloNodeClientService;

    private final YoloNodeService yoloNodeService;

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
}
