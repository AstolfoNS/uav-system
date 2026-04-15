package com.tf.backend.core.infrastructure.repo.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tf.backend.core.mapper.UserRoleMapper;
import com.tf.backend.core.model.entity.UserRoleEntity;
import com.tf.backend.core.infrastructure.repo.UserRoleService;
import org.springframework.stereotype.Service;

@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRoleEntity> implements UserRoleService {
}
