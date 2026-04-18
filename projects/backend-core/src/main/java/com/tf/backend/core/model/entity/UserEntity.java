package com.tf.backend.core.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tf.backend.core.common.enumeration.Gender;
import com.tf.backend.core.model.entity.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@TableName("users")
public class UserEntity extends BaseEntity {

    private String username;

    private String nickname;

    private String avatarUrl;

    private String email;

    private String phoneNumber;

    private String password;

    private Gender gender;

    private String introduction;

    private LocalDateTime lastActiveTime;

    private LocalDateTime lastLoginTime;

}
