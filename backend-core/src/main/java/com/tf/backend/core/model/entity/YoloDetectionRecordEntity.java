package com.tf.backend.core.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.tf.backend.core.common.enumeration.TaskType;
import com.tf.backend.core.model.entity.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * YOLO目标检测历史记录实体类
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@TableName(value = "yolo_detection_records", autoResultMap = true)
@Schema(description = "YOLO目标检测历史记录实体")
public class YoloDetectionRecordEntity extends BaseEntity {

    /**
     * 执行此次预测的节点ID
     */
    @Schema(description = "执行此次预测的节点ID")
    private Long nodeId;

    /**
     * 执行编码 (业务流水号/幂等ID)
     */
    @Schema(description = "执行编码(业务流水号/幂等ID)")
    private String code;

    /**
     * 任务类型：1=图像检测, 2=视频检测
     */
    @Schema(description = "任务类型(1=图像检测,2=视频检测)")
    private TaskType taskType;

    /**
     * 上传的原始文件名
     */
    @Schema(description = "上传的原始文件名")
    private String originalFilename;

    /**
     * FastAPI/MinIO返回的渲染后结果文件远程URL
     */
    @Schema(description = "渲染后结果文件远程URL")
    private String resultUrl;

    /**
     * 检测到的目标总数
     */
    @Schema(description = "检测到的目标总数")
    private Integer detectCount;

    /**
     * 详细检测结果JSON (bbox, confidence, class_name等)
     * 使用 JacksonTypeHandler 自动将其与 MySQL 的 JSON 字段互转
     */
    @Schema(description = "详细检测结果JSON体")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> detectionDetails;

    /**
     * 失败时的错误原因
     */
    @Schema(description = "失败时的错误原因")
    private String errorMessage;

    /**
     * 推理耗时(毫秒)
     */
    @Schema(description = "推理耗时(毫秒)")
    private Long durationMs;
}
