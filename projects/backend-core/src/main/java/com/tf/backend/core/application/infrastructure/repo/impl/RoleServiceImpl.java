package com.tf.backend.core.application.infrastructure.repo.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tf.backend.core.application.mapper.RoleMapper;
import com.tf.backend.core.model.entity.RoleEntity;
import com.tf.backend.core.application.infrastructure.repo.RoleService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, RoleEntity> implements RoleService {

    @Override
    public List<RoleEntity> getByUserId(Long userId) {
        return baseMapper.getByUserId(userId);
    }

}
