package com.tf.backend.core.model.entity.base;

import com.baomidou.mybatisplus.annotation.Version;
import com.tf.backend.core.common.enumeration.Status;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public abstract class BaseEntity extends BaseFields {

    /**
     * 乐观锁版本号
     * 每次更新时自动递增
     */
    @Version
    private Integer optLockVersion;

    /**
     * 数据状态字段
     * 0-禁用
     * 1-正常（默认）
     * 2-锁定
     */
    private Status status;

    private String remark;

}
