package com.tf.backend.core.application.infrastructure.repo;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tf.backend.core.model.entity.RoleEntity;

import java.util.List;

public interface RoleService extends IService<RoleEntity> {

    List<RoleEntity> getByUserId(Long userId);

}
