package com.tf.backend.core.model.entity.base;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Data
@Schema(description = "实体类基础字段")
public abstract class BaseFields implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID - 使用唯一自增ID
     */
    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 创建时间 - 插入时自动填充
     */
    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间 - 插入和更新时自动填充
     */
    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 创建者ID - 插入时自动填充
     * 默认值0表示系统操作
     */
    @Schema(description = "创建者ID")
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    /**
     * 更新者ID - 插入和更新时自动填充
     * 默认值0表示系统操作
     */
    @Schema(description = "更新者ID")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    /**
     * 逻辑删除标记
     * 0-未删除（默认）
     * 1-已删除
     */
    @Schema(description = "逻辑删除标记(0-未删除,1-已删除)")
    @TableLogic(value = "0", delval = "1")
    private Boolean isDeleted;
}
