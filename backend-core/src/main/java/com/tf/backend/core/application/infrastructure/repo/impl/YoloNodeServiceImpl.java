package com.tf.backend.core.application.infrastructure.repo.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tf.backend.core.common.enumeration.Status;
import com.tf.backend.core.common.exception.BizException;
import com.tf.backend.core.application.infrastructure.repo.YoloNodeParamService;
import com.tf.backend.core.application.infrastructure.repo.YoloNodeService;
import com.tf.backend.core.application.infrastructure.repo.YoloWeightService;
import com.tf.backend.core.application.mapper.YoloNodeMapper;
import com.tf.backend.core.model.dto.YoloNodeSaveDTO;
import com.tf.backend.core.model.entity.YoloNodeEntity;
import com.tf.backend.core.model.entity.YoloNodeParamEntity;
import com.tf.backend.core.model.vo.YoloNodeDetailVO;
import com.tf.backend.core.model.vo.YoloNodeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoloNodeServiceImpl extends ServiceImpl<YoloNodeMapper, YoloNodeEntity> implements YoloNodeService {

    private final YoloWeightService yoloWeightService;

    private final YoloNodeParamService yoloNodeParamService;


    @Override
    public IPage<YoloNodeVO> getPageList(Integer current, Integer size, String nodeName, Integer status) {
        Page<YoloNodeEntity> page = new Page<>(current, size);

        LambdaQueryWrapper<YoloNodeEntity> wrapper = Wrappers.<YoloNodeEntity>lambdaQuery()
                .like(StringUtils.hasText(nodeName), YoloNodeEntity::getNodeName, nodeName)
                .eq(status != null, YoloNodeEntity::getStatus, status)
                .orderByDesc(YoloNodeEntity::getUpdatedAt);

        this.page(page, wrapper);

        // 一行代码完成分页信息复制 + 列表数据转换
        return page.convert(YoloNodeVO::mapToVO);
    }

    @Override
    public YoloNodeDetailVO getNodeDetail(Long id) {
        YoloNodeEntity node = this.getById(id);

        if (node == null) {
            throw new IllegalArgumentException("节点不存在");
        }

        List<String> availableWeights = yoloWeightService.listAvailableWeights(id);
        Map<String, Object> currentParams = yoloNodeParamService.getActiveOptByNodeId(id)
                .map(YoloNodeParamEntity::getParams)
                .orElse(null);

        return new YoloNodeDetailVO(
                node.getId(),
                node.getNodeName(),
                node.getDescription(),
                node.getHost(),
                node.getPort(),
                node.getActiveWeightName(),
                node.getStatus(),
                node.getUpdatedAt(),
                availableWeights,
                currentParams
        );
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long createNode(YoloNodeSaveDTO dto) {
        checkNodeNameUnique(null, dto.nodeName());

        YoloNodeEntity node = YoloNodeEntity.builder()
                .nodeName(dto.nodeName())
                .description(dto.description())
                .host(dto.host())
                .port(dto.port())
                .httpProtocol(dto.httpProtocol())
                .apiVersion(dto.apiVersion())
                .build();

        this.save(node);

        log.info("New YOLO node DB record created with ID: {}", node.getId());

        return node.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateNodeInfo(Long id, YoloNodeSaveDTO dto) {
        YoloNodeEntity node = this.getById(id);

        if (node == null) {
            throw new BizException("要修改的节点不存在");
        }

        checkNodeNameUnique(id, dto.nodeName());

        boolean urlChanged = !node.getHost().equals(dto.host()) || !node.getPort().equals(dto.port()) || !node.getHttpProtocol().equals(dto.httpProtocol());

        node.setNodeName(dto.nodeName());
        node.setDescription(dto.description());
        node.setHost(dto.host());
        node.setPort(dto.port());
        node.setHttpProtocol(dto.httpProtocol());
        node.setApiVersion(dto.apiVersion());

        // 如果 URL 改变，立刻将数据库状态设为离线，等待后续探活更新
        if (urlChanged) {
            node.setStatus(Status.DISABLED);
        }

        this.updateById(node);

        log.info("YOLO node [{}] DB record updated.", node.getNodeName());

        return urlChanged;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteNodeWithCascades(Long id) {
        YoloNodeEntity node = this.getById(id);

        if (node == null) {
            return;
        }
        // 删除 yolo-service 节点
        this.removeById(id);
        // 删除该节点存储在数据库表中的权重配置信息
        yoloWeightService.removeByNodeId(id);
        // 删除该节点存储在数据库表中的所有参数模板
        yoloNodeParamService.removeByNodeId(id);

        log.info("YOLO node [{}] and its cascade data deleted successfully.", node.getNodeName());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Map<String, Object> switchActiveTemplate(Long nodeId, String templateName) {
        // 把该节点下现存的正在激活的模板降级为非激活 (is_active = 0)
        YoloNodeParamEntity updateEntity = new YoloNodeParamEntity();

        updateEntity.setIsActive(false);
        yoloNodeParamService.updateActiveByNodeId(nodeId, updateEntity);

        // 找到目标模板，把它设为激活 (is_active = 1)
        YoloNodeParamEntity targetTemplate = yoloNodeParamService.getOptByNodeIdAndTemplateName(nodeId, templateName).orElseThrow(
                () -> new BizException("找不到目标参数模板: " + templateName)
        );

        targetTemplate.setIsActive(true);

        yoloNodeParamService.updateById(targetTemplate);

        // 返回目标模板的参数，供上一层的 YoloNodeManageService 推送给物理机
        return targetTemplate.getParams();
    }

    @Override
    public String getBaseUrlById(Long nodeId) {
        YoloNodeEntity node = getOptById(nodeId).orElseThrow(
                () -> new BizException("目标 YOLO 节点不存在")
        );

        if (!Status.ENABLED.equals(node.getStatus())) {
            throw new BizException("节点当前离线，无法进行操作");
        }

        return node.getBaseUrl();
    }

    @Override
    public void checkNodeNameUnique(Long id, String nodeName) {
        LambdaQueryWrapper<YoloNodeEntity> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(YoloNodeEntity::getNodeName, nodeName);

        if (id != null) {
            wrapper.ne(YoloNodeEntity::getId, id);
        }

        if (this.count(wrapper) > 0) {
            throw new BizException("节点名称已存在，请更换");
        }
    }
}
