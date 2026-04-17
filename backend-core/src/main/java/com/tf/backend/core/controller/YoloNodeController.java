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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "YOLO节点模块", description = "YOLO节点的增删改查、参数同步及权重管理")
public class YoloNodeController {

    private final YoloNodeService yoloNodeService;

    private final YoloNodeManageService yoloNodeManageService;

    private final YoloNodeSyncService yoloNodeSyncService;

    private final YoloNodeClientService yoloNodeClientService;


    @GetMapping("/page")
    @Operation(summary = "分页查询YOLO节点", description = "支持条件过滤")
    public R<IPage<YoloNodeVO>> getPageList(

            @Parameter(description = "当前页码")
            @RequestParam(defaultValue = "1")
            Integer current,

            @Parameter(description = "每页展示数量")
            @RequestParam(defaultValue = "10")
            Integer size,

            @Parameter(description = "节点名称过滤")
            @RequestParam(required = false)
            String nodeName,

            @Parameter(description = "状态过滤")
            @RequestParam(required = false)
            Integer status
    ) {
        return R.ok(yoloNodeService.getPageList(current, size, nodeName, status));
    }


    @GetMapping("/{id}")
    @Operation(summary = "获取节点详情", description = "根据ID获取某个节点的所有详细信息")
    public R<YoloNodeDetailVO> getNodeDetail(

            @Parameter(description = "节点ID", required = true)
            @PathVariable
            Long id
    ) {
        return R.ok(yoloNodeService.getNodeDetail(id));
    }


    @PostMapping
    @Operation(summary = "添加新节点", description = "创建新节点并尝试连接同步")
    public R<Void> addNode(

            @Parameter(description = "节点信息DTO", required = true)
            @Valid
            @RequestBody
            YoloNodeSaveDTO dto
    ) {
        log.info("Request to add new YOLO node: {}", dto.nodeName());

        yoloNodeManageService.addNode(dto);

        return R.okWithMsg("节点添加成功，后台正在尝试连接同步...");
    }


    @PutMapping("/{id}")
    @Operation(summary = "更新节点信息", description = "根据ID更新已存在的节点")
    public R<Void> updateNode(

            @Parameter(description = "节点ID", required = true)
            @PathVariable
            Long id,

            @Parameter(description = "节点信息DTO", required = true)
            @Valid
            @RequestBody
            YoloNodeSaveDTO dto
    ) {
        log.info("Request to update YOLO node ID: {}", id);

        yoloNodeManageService.updateNode(id, dto);

        return R.okWithMsg("节点信息更新成功");
    }


    @DeleteMapping("/{id}")
    @Operation(summary = "删除节点", description = "级联删除某个节点的所有相关信息")
    public R<Void> deleteNode(

            @Parameter(description = "节点ID", required = true)
            @PathVariable
            Long id
    ) {
        log.info("Request to delete YOLO node ID: {}", id);

        yoloNodeService.deleteNodeWithCascades(id);

        return R.okWithMsg("节点及其关联数据删除成功");
    }

    /**
     * 一键切换并应用参数模板
     */
    @PutMapping("/{id}/params/template/{templateName}/apply")
    @Operation(summary = "应用参数模板", description = "一键切换并应用特定的检测参数模板下发到特定物理节点")
    public R<Void> applyParamTemplate(

            @Parameter(description = "节点ID", required = true)
            @PathVariable
            Long id,

            @Parameter(description = "模板名称", required = true)
            @PathVariable
            String templateName

    ) {
        log.info("Applying template [{}] to node ID: {}", templateName, id);

        yoloNodeManageService.applyParamTemplate(id, templateName);

        return R.okWithMsg("参数模板已成功下发并应用");
    }

    /**
     * 手动触发节点状态全量同步 (防漂移)
     */
    @PostMapping("/{id}/sync")
    @Operation(summary = "手动全量同步节点", description = "强制从YOLO服务拉取全量参数并覆写数据库防止漂移")
    public R<Void> manualSyncNodeState(

            @Parameter(description = "节点ID", required = true)
            @PathVariable
            Long id

    ) {
        log.info("Manual sync triggered for node ID: {}", id);

        yoloNodeSyncService.syncNodeState(id);

        return R.okWithMsg("节点状态同步成功");
    }


    @PostMapping("/{id}/weights")
    @Operation(summary = "上传模型权重", description = "直接通过该控制层向YOLO节点物理上传.pt文件")
    public R<Void> uploadWeight(

            @Parameter(description = "节点ID", required = true)
            @PathVariable
            Long id,

            @Parameter(description = "模型权重文件(.pt)", required = true)
            @RequestParam("file")
            MultipartFile file

    ) {
        log.info("Uploading weight {} to node ID: {}", file.getOriginalFilename(), id);

        yoloNodeClientService.uploadWeight(yoloNodeService.getBaseUrlById(id), file);
        // 上传成功后，立刻同步刷回数据库
        yoloNodeSyncService.syncNodeState(id);

        return R.okWithMsg("模型权重上传成功");
    }


    @PutMapping("/{id}/weights/{filename}/active")
    @Operation(summary = "切换活跃模型", description = "切换指定的YOLO节点目前在使用的权重文件")
    public R<Void> changeActiveWeight(

            @Parameter(description = "节点ID", required = true)
            @PathVariable
            Long id,

            @Parameter(description = "权重文件名", required = true)
            @PathVariable
            String filename

    ) {
        log.info("Changing active weight for node ID: {} to {}", id, filename);

        yoloNodeClientService.changeActiveWeight(yoloNodeService.getBaseUrlById(id), filename);
        yoloNodeSyncService.syncNodeState(id);

        return R.okWithMsg("活跃模型切换成功");
    }


    @DeleteMapping("/{id}/weights/{filename}")
    @Operation(summary = "删除模型权重", description = "将模型文件从物理机器上删除")
    public R<Void> deleteWeight(

            @Parameter(description = "节点ID", required = true)
            @PathVariable
            Long id,

            @Parameter(description = "权重文件名", required = true)
            @PathVariable
            String filename

    ) {
        log.info("Deleting weight {} from node ID: {}", filename, id);

        yoloNodeClientService.deleteWeight(yoloNodeService.getBaseUrlById(id), filename);
        yoloNodeSyncService.syncNodeState(id);

        return R.okWithMsg("模型权重删除成功");
    }


}
