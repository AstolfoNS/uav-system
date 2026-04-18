package com.tf.backend.core.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tf.backend.core.application.domain.yolo.YoloInferenceService;
import com.tf.backend.core.application.infrastructure.repo.YoloDetectionRecordService;
import com.tf.backend.core.common.enumeration.TaskStatus;
import com.tf.backend.core.common.response.R;
import com.tf.backend.core.model.entity.YoloDetectionRecordEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * YOLO 目标检测推理接口
 */
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/inference")
@RestController
@Tag(name = "推理模块", description = "YOLO目标检测推理接口")
public class YoloInferenceController {

    private final YoloInferenceService yoloInferenceService;

    private final YoloDetectionRecordService yoloDetectionRecordService;

    /**
     * 发起图像检测预测
     */
    @PostMapping("/nodes/{nodeId}/image")
    @PreAuthorize("hasAuthority('inference:image:run')")
    @Operation(summary = "图像检测预测", description = "向指定节点发起图像检测任务")
    public R<YoloDetectionRecordEntity> predictImage(

            @Parameter(description = "节点ID", required = true)
            @PathVariable
            Long nodeId,

            @Parameter(description = "图片文件", required = true)
            @RequestParam("file")
            MultipartFile file

    ) {
        log.info("Received image prediction request for node [{}] with file [{}]", nodeId, file.getOriginalFilename());
        
        // 调用业务服务，它会同步阻塞等待 FastAPI 返回，并完成落库
        YoloDetectionRecordEntity record = yoloInferenceService.predictImage(nodeId, file);
        
        if (record.getTaskStatus() == TaskStatus.FAILED) {
            return R.failed("图像检测失败: " + record.getErrorMessage());
        }
        
        return R.ok(record, "图像检测成功");
    }

    /**
     * 发起视频检测预测
     */
    @PostMapping("/nodes/{nodeId}/video")
    @PreAuthorize("hasAuthority('inference:video:run')")
    @Operation(summary = "视频检测预测", description = "向指定节点发起视频检测任务")
    public R<YoloDetectionRecordEntity> predictVideo(

            @Parameter(description = "节点ID", required = true)
            @PathVariable
            Long nodeId,

            @Parameter(description = "视频文件", required = true)
            @RequestParam("file")
            MultipartFile file

    ) {
        log.info("Received video prediction request for node [{}] with file [{}]", nodeId, file.getOriginalFilename());

        YoloDetectionRecordEntity record = yoloInferenceService.predictVideo(nodeId, file);

        if (record.getTaskStatus() == TaskStatus.FAILED) {
            return R.failed("视频检测失败: " + record.getErrorMessage());
        }

        return R.ok(record, "视频检测及渲染已完成");
    }

    /**
     * 分页查询检测历史记录 (供前端大屏或表格展示)
     */
    @GetMapping("/records/page")
    @PreAuthorize("hasAuthority('inference:record:page')")
    @Operation(summary = "分页查询检测历史记录", description = "供前端大屏或表格展示")
    public R<IPage<YoloDetectionRecordEntity>> getRecordPage(

            @Parameter(description = "当前页码")
            @RequestParam(defaultValue = "1")
            Integer current,

            @Parameter(description = "每页展示数量")
            @RequestParam(defaultValue = "10")
            Integer size,

            @Parameter(description = "节点ID过滤")
            @RequestParam(required = false)
            Long nodeId,

            @Parameter(description = "任务类型过滤")
            @RequestParam(required = false)
            Integer taskType,

            @Parameter(description = "原始文件名过滤")
            @RequestParam(required = false)
            String originalFilename

    ) {
        IPage<YoloDetectionRecordEntity> pageRecords =
                yoloDetectionRecordService.getRecordPage(current, size, nodeId, taskType, originalFilename);

        return R.ok(pageRecords);
    }

    /**
     * 获取单条预测记录的详细信息
     */
    @GetMapping("/records/{id}")
    @PreAuthorize("hasAuthority('inference:record:detail')")
    @Operation(summary = "获取单条预测记录及详细信息", description = "通过ID查询")
    public R<YoloDetectionRecordEntity> getRecordDetail(

            @Parameter(description = "记录ID", required = true)
            @PathVariable
            Long id

    ) {
        YoloDetectionRecordEntity record = yoloDetectionRecordService.getById(id);

        if (record == null) {
            return R.failed("该检测记录不存在");
        }

        return R.ok(record);
    }

    /**
     * 删除指定的预测记录 (支持清理历史)
     */
    @DeleteMapping("/records/{id}")
    @PreAuthorize("hasAuthority('inference:record:delete')")
    @Operation(summary = "删除指定的预测记录", description = "删除单条检测历史支持清理历史")
    public R<Void> deleteRecord(

            @Parameter(description = "记录ID", required = true)
            @PathVariable
            Long id

    ) {
        yoloDetectionRecordService.removeById(id);

        return R.okWithMsg("记录删除成功");
    }


}