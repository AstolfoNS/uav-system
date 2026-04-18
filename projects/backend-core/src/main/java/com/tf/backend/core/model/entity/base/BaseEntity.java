package com.tf.backend.core.model.entity.base;

import com.baomidou.mybatisplus.annotation.Version;
import com.tf.backend.core.common.enumeration.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "基础业务实体")
public abstract class BaseEntity extends BaseFields {

    /**
     * 乐观锁版本号
     * 每次更新时自动递增
     */
    @Schema(description = "乐观锁版本号")
    @Version
    private Integer optLockVersion;

    /**
     * 数据状态字段
     * 0-禁用
     * 1-正常（默认）
     * 2-锁定
     */
    @Schema(description = "数据状态(0-禁用,1-正常,2-锁定)")
    private Status status;

    @Schema(description = "备注")
    private String remark;
}
