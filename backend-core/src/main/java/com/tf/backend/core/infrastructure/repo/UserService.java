package com.tf.backend.core.infrastructure.repo;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tf.backend.core.model.entity.UserEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

public interface UserService extends IService<UserEntity> {

    @Async("dbExecutor")
    void updateLastLoginTimeAsync(Long userId);

    @Transactional(rollbackFor = Exception.class)
    void batchUpdateLastActiveTime(Map<Long, LocalDateTime> userActiveTimeMap);

}
