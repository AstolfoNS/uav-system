package com.tf.backend.core.application.domain.rbac;

import com.tf.backend.core.model.dto.RolePermissionUpdateDTO;
import com.tf.backend.core.model.dto.RoleCreateDTO;
import com.tf.backend.core.model.dto.UserRoleUpdateDTO;

public interface AdminRbacCommandService {

    void createRole(RoleCreateDTO dto);

    void updateUserRoles(Long userId, UserRoleUpdateDTO dto);

    void updateRolePermissions(Long roleId, RolePermissionUpdateDTO dto);
}
