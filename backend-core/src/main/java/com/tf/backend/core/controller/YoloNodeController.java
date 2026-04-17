package com.tf.backend.core.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tf.backend.core.common.response.R;
import com.tf.backend.core.application.domain.yolo.YoloNodeClientService;
import com.tf.backend.core.application.domain.yolo.YoloNodeManageService;
import com.tf.backend.core.application.domain.yolo.YoloNodeSyncService;
import com.tf.backend.core.application.infrastructure.repo.YoloNodeService;
import com.tf.backend.core.model.dto.YoloNodeParamCreateDTO;
import com.tf.backend.core.model.dto.YoloNodeParamUpdateDTO;
import com.tf.backend.core.model.dto.YoloNodeWeightsDTO;
import com.tf.backend.core.model.dto.YoloNodeSaveDTO;
import com.tf.backend.core.model.vo.YoloNodeParamVO;
import com.tf.backend.core.model.vo.YoloNodeDetailVO;
import com.tf.backend.core.model.vo.YoloNodeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
    @PreAuthorize("hasAuthority('model:node:page')")
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
    @PreAuthorize("hasAuthority('model:node:detail')")
    @Operation(summary = "获取节点详情", description = "根据ID获取某个节点的所有详细信息")
    public R<YoloNodeDetailVO> getNodeDetail(

            @Parameter(description = "节点ID", required = true)
            @PathVariable
            Long id
    ) {
        return R.ok(yoloNodeService.getNodeDetail(id));
    }


    @PostMapping
    @PreAuthorize("hasAuthority('model:node:create')")
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
    @PreAuthorize("hasAuthority('model:node:update')")
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
    @PreAuthorize("hasAuthority('model:node:delete')")
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
    @PreAuthorize("hasAuthority('model:param:apply')")
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

    @GetMapping("/{id}/params/templates")
    @PreAuthorize("hasAuthority('model:param:list')")
    @Operation(summary = "查询参数模板列表", description = "返回某个节点下的全部参数模板，包含激活状态与参数内容")
    public R<List<YoloNodeParamVO>> listParamTemplates(

            @Parameter(description = "节点ID", required = true)
            @PathVariable
            Long id
    ) {
        return R.ok(yoloNodeManageService.listParamTemplates(id));
    }

    @GetMapping("/{id}/params/templates/{templateName}")
    @PreAuthorize("hasAuthority('model:param:detail')")
    @Operation(summary = "查询单个参数模板", description = "根据模板名称返回模板详情")
    public R<YoloNodeParamVO> getParamTemplate(

            @Parameter(description = "节点ID", required = true)
            @PathVariable
            Long id,

            @Parameter(description = "模板名称", required = true)
            @PathVariable
            String templateName
    ) {
        return R.ok(yoloNodeManageService.getParamTemplate(id, templateName));
    }

    @PostMapping("/{id}/params/templates")
    @PreAuthorize("hasAuthority('model:param:create')")
    @Operation(summary = "创建参数模板", description = "为指定节点创建一个新的参数模板")
    public R<Void> createParamTemplate(

            @Parameter(description = "节点ID", required = true)
            @PathVariable
            Long id,

            @Valid
            @RequestBody
            YoloNodeParamCreateDTO dto
    ) {
        yoloNodeManageService.createParamTemplate(id, dto);
        return R.okWithMsg("参数模板创建成功");
    }

    @PutMapping("/{id}/params/templates/{templateName}")
    @PreAuthorize("hasAuthority('model:param:update')")
    @Operation(summary = "更新参数模板", description = "修改模板描述、参数内容或激活状态")
    public R<Void> updateParamTemplate(

            @Parameter(description = "节点ID", required = true)
            @PathVariable
            Long id,

            @Parameter(description = "模板名称", required = true)
            @PathVariable
            String templateName,

            @Valid
            @RequestBody
            YoloNodeParamUpdateDTO dto
    ) {
        yoloNodeManageService.updateParamTemplate(id, templateName, dto);
        return R.okWithMsg("参数模板更新成功");
    }

    @DeleteMapping("/{id}/params/templates/{templateName}")
    @PreAuthorize("hasAuthority('model:param:delete')")
    @Operation(summary = "删除参数模板", description = "删除指定节点下的参数模板")
    public R<Void> deleteParamTemplate(

            @Parameter(description = "节点ID", required = true)
            @PathVariable
            Long id,

            @Parameter(description = "模板名称", required = true)
            @PathVariable
            String templateName
    ) {
        yoloNodeManageService.deleteParamTemplate(id, templateName);
        return R.okWithMsg("参数模板删除成功");
    }

    @GetMapping("/{id}/weights")
    @PreAuthorize("hasAuthority('model:weight:list')")
    @Operation(summary = "查询权重列表", description = "返回节点当前激活权重和可用权重文件列表")
    public R<YoloNodeWeightsDTO> getWeightSummary(

            @Parameter(description = "节点ID", required = true)
            @PathVariable
            Long id
    ) {
        return R.ok(yoloNodeManageService.getWeightSummary(id));
    }

    /**
     * 手动触发节点状态全量同步 (防漂移)
     */
    @PostMapping("/{id}/sync")
    @PreAuthorize("hasAuthority('model:node:sync')")
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
    @PreAuthorize("hasAuthority('model:weight:upload')")
    @Operation(summary = "上传模型权重", description = "直接通过该控制层向YOLO节点物理上传权重文件，支持 .pt/.onnx")
    public R<Void> uploadWeight(

            @Parameter(description = "节点ID", required = true)
            @PathVariable
            Long id,

            @Parameter(description = "模型权重文件(.pt/.onnx)", required = true)
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
    @PreAuthorize("hasAuthority('model:weight:switch')")
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
    @PreAuthorize("hasAuthority('model:weight:delete')")
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
