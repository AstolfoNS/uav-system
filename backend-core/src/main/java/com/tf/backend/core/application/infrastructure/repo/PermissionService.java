package com.tf.backend.core.application.infrastructure.repo;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tf.backend.core.common.enumeration.PermType;
import com.tf.backend.core.model.entity.PermissionEntity;

import java.util.List;

public interface PermissionService extends IService<PermissionEntity> {

    List<PermissionEntity> getByUserId(Long userId);

    List<PermissionEntity> getByUserIdAndPermType(Long userId, PermType permType);

}
