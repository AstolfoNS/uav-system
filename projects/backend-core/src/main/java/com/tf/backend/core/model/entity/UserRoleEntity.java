package com.tf.backend.core.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tf.backend.core.model.entity.base.BaseFields;
import lombok.*;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@TableName("user_roles")
public class UserRoleEntity extends BaseFields {

    private Long userId;

    private Long roleId;

}
