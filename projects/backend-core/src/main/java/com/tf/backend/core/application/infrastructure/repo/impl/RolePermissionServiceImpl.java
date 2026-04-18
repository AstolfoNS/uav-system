package com.tf.backend.core.application.infrastructure.repo.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tf.backend.core.application.mapper.RolePermissionMapper;
import com.tf.backend.core.model.entity.RolePermissionEntity;
import com.tf.backend.core.application.infrastructure.repo.RolePermissionService;
import org.springframework.stereotype.Service;

@Service
public class RolePermissionServiceImpl extends ServiceImpl<RolePermissionMapper, RolePermissionEntity> implements RolePermissionService {
}
