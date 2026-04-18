package com.tf.backend.core.application.security;

import com.tf.backend.core.model.entity.PermissionEntity;
import com.tf.backend.core.model.entity.RoleEntity;
import com.tf.backend.core.model.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    private List<String> roles;

    private List<String> permissions;


    public static LoginUser of(UserEntity user, List<RoleEntity> roles, List<PermissionEntity> permissions) {
        return builder()
                .id(user.getId())
                .username(user.getUsername())
                .roles(roles.stream().map(RoleEntity::getCode).toList())
                .permissions(permissions.stream().map(PermissionEntity::getCode).toList())
                .build();
    }

}
