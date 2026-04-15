package com.tf.backend.core.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tf.backend.core.model.entity.base.BaseFields;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@TableName("user_roles")
public class UserRoleEntity extends BaseFields {

    private Long userId;

    private Long roleId;

}
