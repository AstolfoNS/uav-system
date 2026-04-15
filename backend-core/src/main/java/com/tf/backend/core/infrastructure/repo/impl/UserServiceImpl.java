package com.tf.backend.core.infrastructure.repo.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tf.backend.core.mapper.UserMapper;
import com.tf.backend.core.model.entity.UserEntity;
import com.tf.backend.core.infrastructure.repo.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {
    /**
     * 异步更新最后登录时间（用于登录、刷新Token成功后）
     */
    @Async("dbExecutor")
    @Override
    public void updateLastLoginTimeAsync(Long userId) {
        UserEntity updateEntity = new UserEntity();
        updateEntity.setId(userId);
        updateEntity.setLastLoginTime(LocalDateTime.now());
        this.updateById(updateEntity);
    }

    /**
     * 批量更新最后活跃时间
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void batchUpdateLastActiveTime(Map<Long, LocalDateTime> userActiveTimeMap) {
        if (userActiveTimeMap == null || userActiveTimeMap.isEmpty()) {
            return;
        }
        List<UserEntity> updateList = new ArrayList<>();
        userActiveTimeMap.forEach((userId, activeTime) -> {
            UserEntity entity = new UserEntity();
            entity.setId(userId);
            entity.setLastActiveTime(activeTime);
            updateList.add(entity);
        });
        this.updateBatchById(updateList);
    }
}
