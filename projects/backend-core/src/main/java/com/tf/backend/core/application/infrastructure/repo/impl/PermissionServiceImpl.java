package com.tf.backend.core.application.infrastructure.repo.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tf.backend.core.common.enumeration.PermType;
import com.tf.backend.core.application.mapper.PermissionMapper;
import com.tf.backend.core.model.entity.PermissionEntity;
import com.tf.backend.core.application.infrastructure.repo.PermissionService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, PermissionEntity> implements PermissionService {

    @Override
    public List<PermissionEntity> getByUserId(Long userId) {
        return baseMapper.getByUserId(userId);
    }

    @Override
    public List<PermissionEntity> getByUserIdAndPermType(Long userId, PermType permType) {
        return baseMapper.getByUserId(userId).stream()
                .filter(p -> p.getType().equals(permType))
                .collect(Collectors.toList());
    }

}
