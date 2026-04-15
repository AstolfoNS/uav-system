package com.tf.backend.core.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tf.backend.core.common.enumeration.PermType;
import com.tf.backend.core.model.entity.base.BaseEntity;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@TableName("permissions")
public class PermissionEntity extends BaseEntity {

    private String code;

    private String name;

    private PermType type;

    private String description;

    private Integer sortOrder;

}
