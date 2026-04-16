package com.tf.backend.core.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tf.backend.core.common.response.R;
import com.tf.backend.core.application.domain.yolo.YoloNodeClientService;
import com.tf.backend.core.application.domain.yolo.YoloNodeManageService;
import com.tf.backend.core.application.domain.yolo.YoloNodeSyncService;
import com.tf.backend.core.application.infrastructure.repo.YoloNodeService;
import com.tf.backend.core.model.dto.YoloNodeSaveDTO;
import com.tf.backend.core.model.vo.YoloNodeDetailVO;
import com.tf.backend.core.model.vo.YoloNodeVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * YOLO 节点管理控制器 (RESTful API)
 */
@Slf4j
@RestController
@RequestMapping("/yolo-nodes")
@RequiredArgsConstructor
public class YoloNodeController {

    private final YoloNodeService yoloNodeService;

    private final YoloNodeManageService yoloNodeManageService;

    private final YoloNodeSyncService yoloNodeSyncService;

    private final YoloNodeClientService yoloNodeClientService;

    // ================= 基础信息管理 (本地数据库 CRUD) =================

    @GetMapping("/page")
    public R<IPage<YoloNodeVO>> getPageList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String nodeName,
            @RequestParam(required = false) Integer status
    ) {
        return R.ok(yoloNodeService.getPageList(current, size, nodeName, status));
    }

    @GetMapping("/{id}")
    public R<YoloNodeDetailVO> getNodeDetail(@PathVariable Long id) {
        return R.ok(yoloNodeService.getNodeDetail(id));
    }

    @PostMapping
    public R<Void> addNode(@Valid @RequestBody YoloNodeSaveDTO dto) {
        log.info("Request to add new YOLO node: {}", dto.nodeName());

        yoloNodeManageService.addNode(dto);

        return R.okWithMsg("节点添加成功，后台正在尝试连接同步...");
    }

    @PutMapping("/{id}")
    public R<Void> updateNode(@PathVariable Long id, @Valid @RequestBody YoloNodeSaveDTO dto) {
        log.info("Request to update YOLO node ID: {}", id);
        yoloNodeManageService.updateNode(id, dto);
        return R.okWithMsg("节点信息更新成功");
    }

    @DeleteMapping("/{id}")
    public R<Void> deleteNode(@PathVariable Long id) {
        log.info("Request to delete YOLO node ID: {}", id);

        yoloNodeService.deleteNodeWithCascades(id);

        return R.okWithMsg("节点及其关联数据删除成功");
    }

    // ================= 参数模板与状态同步 (高阶编排指令) =================

    /**
     * 一键切换并应用参数模板
     */
    @PutMapping("/{id}/params/template/{templateName}/apply")
    public R<Void> applyParamTemplate(@PathVariable Long id, @PathVariable String templateName) {
        log.info("Applying template [{}] to node ID: {}", templateName, id);

        yoloNodeManageService.applyParamTemplate(id, templateName);

        return R.okWithMsg("参数模板已成功下发并应用");
    }

    /**
     * 手动触发节点状态全量同步 (防漂移)
     */
    @PostMapping("/{id}/sync")
    public R<Void> manualSyncNodeState(@PathVariable Long id) {
        log.info("Manual sync triggered for node ID: {}", id);

        yoloNodeSyncService.syncNodeState(id);

        return R.okWithMsg("节点状态同步成功");
    }

    // ================= 模型权重物理管理 (物理机直接交互) =================

    @PostMapping("/{id}/weights")
    public R<Void> uploadWeight(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        log.info("Uploading weight {} to node ID: {}", file.getOriginalFilename(), id);

        yoloNodeClientService.uploadWeight(yoloNodeService.getBaseUrlById(id), file);
        // 上传成功后，立刻同步刷回数据库
        yoloNodeSyncService.syncNodeState(id);

        return R.okWithMsg("模型权重上传成功");
    }

    @PutMapping("/{id}/weights/{filename}/active")
    public R<Void> changeActiveWeight(@PathVariable Long id, @PathVariable String filename) {
        log.info("Changing active weight for node ID: {} to {}", id, filename);

        yoloNodeClientService.changeActiveWeight(yoloNodeService.getBaseUrlById(id), filename);
        yoloNodeSyncService.syncNodeState(id);

        return R.okWithMsg("活跃模型切换成功");
    }

    @DeleteMapping("/{id}/weights/{filename}")
    public R<Void> deleteWeight(@PathVariable Long id, @PathVariable String filename) {
        log.info("Deleting weight {} from node ID: {}", filename, id);

        yoloNodeClientService.deleteWeight(yoloNodeService.getBaseUrlById(id), filename);
        yoloNodeSyncService.syncNodeState(id);

        return R.okWithMsg("模型权重删除成功");
    }
}
