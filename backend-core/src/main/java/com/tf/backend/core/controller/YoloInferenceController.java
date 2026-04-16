package com.tf.backend.core.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tf.backend.core.application.domain.yolo.YoloInferenceService;
import com.tf.backend.core.application.infrastructure.repo.YoloDetectionRecordService;
import com.tf.backend.core.common.enumeration.Status;
import com.tf.backend.core.common.response.R;
import com.tf.backend.core.model.entity.YoloDetectionRecordEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * YOLO 目标检测推理接口
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/inference")
public class YoloInferenceController {

    private final YoloInferenceService yoloInferenceService;

    private final YoloDetectionRecordService yoloDetectionRecordService;

    /**
     * 发起图像检测预测
     */
    @PostMapping("/nodes/{nodeId}/image")
    public R<YoloDetectionRecordEntity> predictImage(@PathVariable Long nodeId, @RequestParam("file") MultipartFile file) {
        log.info("Received image prediction request for node [{}] with file [{}]", nodeId, file.getOriginalFilename());
        
        // 调用业务服务，它会同步阻塞等待 FastAPI 返回，并完成落库
        YoloDetectionRecordEntity record = yoloInferenceService.predictImage(nodeId, file);
        
        // 如果状态为 0，说明失败了，向前端返回错误信息
        if (record.getStatus() == Status.DISABLED) {
            return R.failed("图像检测失败: " + record.getErrorMessage());
        }
        
        return R.ok(record, "图像检测成功");
    }

    /**
     * 发起视频检测预测
     */
    @PostMapping("/nodes/{nodeId}/video")
    public R<YoloDetectionRecordEntity> predictVideo(@PathVariable Long nodeId, @RequestParam("file") MultipartFile file) {
        log.info("Received video prediction request for node [{}] with file [{}]", nodeId, file.getOriginalFilename());

        YoloDetectionRecordEntity record = yoloInferenceService.predictVideo(nodeId, file);

        if (record.getStatus() == Status.DISABLED) {
            return R.failed("视频检测失败: " + record.getErrorMessage());
        }

        return R.ok(record, "视频检测及渲染已完成");
    }


    // ================= 2. 预测历史记录查询接口 =================

    /**
     * 分页查询检测历史记录 (供前端大屏或表格展示)
     */
    @GetMapping("/records/page")
    public R<IPage<YoloDetectionRecordEntity>> getRecordPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long nodeId,
            @RequestParam(required = false) Integer taskType,
            @RequestParam(required = false) String originalFilename
    ) {
        IPage<YoloDetectionRecordEntity> pageRecords = yoloDetectionRecordService.getRecordPage(current, size, nodeId, taskType, originalFilename);

        return R.ok(pageRecords);
    }

    /**
     * 获取单条预测记录的详细信息
     */
    @GetMapping("/records/{id}")
    public R<YoloDetectionRecordEntity> getRecordDetail(@PathVariable Long id) {
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
    public R<Void> deleteRecord(@PathVariable Long id) {
        yoloDetectionRecordService.removeById(id);

        return R.okWithMsg("记录删除成功");
    }
}