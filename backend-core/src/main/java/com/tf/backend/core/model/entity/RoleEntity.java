package com.tf.backend.core.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tf.backend.core.model.entity.base.BaseEntity;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@TableName("roles")
public class RoleEntity extends BaseEntity {

    private String code;

    private String name;

    private String description;

    private Integer level;

    private Integer sortOrder;

}
