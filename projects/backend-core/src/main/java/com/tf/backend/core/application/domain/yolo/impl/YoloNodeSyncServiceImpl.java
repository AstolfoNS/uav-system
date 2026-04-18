package com.tf.backend.core.application.domain.yolo.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tf.backend.core.common.enumeration.Status;
import com.tf.backend.core.common.util.UrlUtils;
import com.tf.backend.core.application.infrastructure.repo.YoloNodeParamService;
import com.tf.backend.core.application.infrastructure.repo.YoloNodeService;
import com.tf.backend.core.application.infrastructure.repo.YoloWeightService;
import com.tf.backend.core.model.dto.YoloNodeModelDTO;
import com.tf.backend.core.model.dto.YoloNodeWeightsDTO;
import com.tf.backend.core.model.entity.YoloNodeEntity;
import com.tf.backend.core.model.entity.YoloNodeParamEntity;
import com.tf.backend.core.model.entity.YoloWeightEntity;
import com.tf.backend.core.application.domain.yolo.YoloNodeSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoloNodeSyncServiceImpl implements YoloNodeSyncService {

    private final YoloNodeService yoloNodeService;

    private final YoloNodeParamService yoloNodeParamService;

    private final YoloWeightService yoloWeightService;

    private final OkHttpClient okHttpClient;

    private final JsonMapper jsonMapper;

    private final TransactionTemplate transactionTemplate;


    @Override
    public void checkAllNodesHealth() {
        List<YoloNodeEntity> nodes = yoloNodeService.list();

        nodes.parallelStream().forEach(node -> {
            Status expectedStatus = checkSingleNodeHealth(node.getBaseUrl()) ? Status.ENABLED : Status.DISABLED;

            if (!expectedStatus.equals(node.getStatus())) {
                // 状态发生了改变
                Status oldStatus = node.getStatus();
                node.setStatus(expectedStatus);
                yoloNodeService.updateById(node);

                log.info("Updated node {} status from {} to {}", node.getNodeName(), oldStatus, expectedStatus);

                // 如果节点刚刚“复活/上线”，立刻同步一次它的物理状态（权重和参数）
                if (Status.ENABLED.equals(expectedStatus)) {
                    try {
                        log.info("Node {} came online, triggering physical state sync...", node.getNodeName());

                        syncNodeState(node.getId());
                    } catch (Exception e) {
                        log.error("Failed to sync state for newly online node {}", node.getNodeName(), e);
                    }
                }
            }
        });
    }

    @Override
    public void syncNodeState(Long nodeId) {
        YoloNodeEntity node = yoloNodeService.getOptById(nodeId).orElseThrow(
                () -> new RuntimeException("Node with id " + nodeId + " not found")
        );

        try {
            YoloNodeModelDTO model = fetchNodeModel(node.getBaseUrl());

            transactionTemplate.executeWithoutResult(
                    _ -> updateNodeStateInDb(node, model.weights().activeWeight(), model.weights().availableWeights(), model.paramsMap())
            );
        } catch (Exception e) {
            log.error("Failed to sync state for node {}", node.getNodeName(), e);

            throw new RuntimeException("Node physical state synchronization failed", e);
        }
    }

    private boolean isHealth(JsonNode rootNode) {
        return rootNode.has("status") && "ok".equals(rootNode.get("status").asString());
    }

    // ================== 私有方法区：网络请求 (纯 IO) ==================

    private boolean checkSingleNodeHealth(String baseUrl) {
        Request request = new Request.Builder()
                .url(UrlUtils.buildUrl(baseUrl, "/health"))
                .get()
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return isHealth(jsonMapper.readTree(response.body().string()));
            }
        } catch (Exception e) {
            log.warn("Node health check failed for {}: {}", baseUrl, e.getMessage());
        }

        return false;
    }

    private YoloNodeWeightsDTO fetchWeights(String baseUrl) throws Exception {
        Request request = new Request.Builder()
                .url(UrlUtils.buildUrl(baseUrl, "/weights/"))
                .get()
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Failed to fetch weights HTTP status " + response.code());
            }

            JsonNode root = jsonMapper.readTree(response.body().string());

            if (root.get("code").asInt() != 200) {
                throw new RuntimeException("API error: " + root.get("message").asString());
            }

            JsonNode data = root.get("data");

            String activeWeight = data.has("active") && !data.get("active").isNull() ? data.get("active").asString() : null;
            List<String> availableWeights = new ArrayList<>();

            JsonNode availableNode = data.get("available");

            if (availableNode != null && availableNode.isArray()) {
                availableNode.forEach(node -> availableWeights.add(node.asString()));
            }

            return new YoloNodeWeightsDTO(activeWeight, availableWeights);
        }
    }

    private Map<String, Object> fetchParams(String baseUrl) throws Exception {
        Request request = new Request.Builder()
                .url(UrlUtils.buildUrl(baseUrl, "/params/"))
                .get()
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Failed to fetch params HTTP status " + response.code());
            }

            JsonNode root = jsonMapper.readTree(response.body().string());

            if (root.get("code").asInt() != 200) {
                throw new RuntimeException("API error: " + root.get("message").asString());
            }

            return jsonMapper.convertValue(root.get("data"), new TypeReference<>() {});
        }
    }

    private YoloNodeModelDTO fetchNodeModel(String baseUrl) throws Exception {
        return new YoloNodeModelDTO(fetchWeights(baseUrl), fetchParams(baseUrl));
    }

    // ================== 私有方法区：数据库操作 (事务内) ==================

    private void updateNodeStateInDb(YoloNodeEntity node, String activeWeight, List<String> availableWeights, Map<String, Object> paramsMap) {
        // 更新 Node 自身 Active 字段
        if (!Objects.equals(node.getActiveWeightName(), activeWeight)) {
            node.setActiveWeightName(activeWeight);

            yoloNodeService.updateById(node);
        }

        // 清理旧数据，批量插入新权重
        yoloWeightService.remove(new LambdaQueryWrapper<YoloWeightEntity>().eq(YoloWeightEntity::getNodeId, node.getId()));

        if (!availableWeights.isEmpty()) {
            List<YoloWeightEntity> weightEntities = availableWeights.stream().map(filename ->
                    YoloWeightEntity.builder()
                            .nodeId(node.getId())
                            .filename(filename)
                            .isActive(Objects.equals(filename, activeWeight))
                            .build()
            ).collect(Collectors.toList());

            yoloWeightService.saveBatch(weightEntities);
        }

        // 更新或插入参数：只查找当前被激活的那个模板 (is_active = 1)
        yoloNodeParamService.getActiveOptByNodeId(node.getId()).ifPresentOrElse(
                (entity) -> {
                    // 更新从数据库查出来的这个 entity
                    entity.setParams(paramsMap);

                    yoloNodeParamService.updateById(entity);
                },
                () -> {
                    YoloNodeParamEntity newParams = YoloNodeParamEntity.builder()
                            .nodeId(node.getId())
                            .isActive(true)
                            .params(paramsMap)
                            .build();

                    yoloNodeParamService.save(newParams);
                }
        );
    }
}
