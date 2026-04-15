package com.tf.backend.core.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tf.backend.core.model.entity.base.BaseFields;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@TableName("role_permissions")
public class RolePermissionEntity extends BaseFields {

    private Long roleId;

    private Long permissionId;

}
