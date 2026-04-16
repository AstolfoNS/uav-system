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
import java.time.LocalDateTime;
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

    private String nickname;

    private String password;

    private String email;

    private String phoneNumber;

    private LocalDateTime lastLoginTime;

    private List<String> roles;

    private List<String> permissions;


    public static LoginUser from(UserEntity user, List<RoleEntity> roles, List<PermissionEntity> permissions) {
        return builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .password(user.getPassword())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .lastLoginTime(user.getLastLoginTime())
                .roles(roles.stream().map(RoleEntity::getCode).toList())
                .permissions(permissions.stream().map(PermissionEntity::getCode).toList())
                .build();
    }

}
