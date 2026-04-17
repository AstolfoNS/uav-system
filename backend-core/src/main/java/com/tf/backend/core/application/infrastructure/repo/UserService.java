package com.tf.backend.core.application.infrastructure.repo;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tf.backend.core.model.dto.UserPasswordUpdateDTO;
import com.tf.backend.core.model.dto.UserProfileUpdateDTO;
import com.tf.backend.core.model.entity.UserEntity;
import com.tf.backend.core.model.vo.UserProfileVO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface UserService extends IService<UserEntity> {

    @Async("dbExecutor")
    void updateLastLoginTimeAsync(Long userId);

    @Transactional(rollbackFor = Exception.class)
    void batchUpdateLastActiveTime(Map<Long, LocalDateTime> userActiveTimeMap);

    @Transactional(rollbackFor = Exception.class)
    void updateUserProfile(Long userId, UserProfileUpdateDTO dto);

    UserProfileVO getCurrentUserProfile(Long userId, List<String> roles, List<String> permissions);

    @Transactional(rollbackFor = Exception.class)
    UserProfileVO updateCurrentUserProfile(Long userId, List<String> roles, List<String> permissions, UserProfileUpdateDTO dto);

    @Transactional(rollbackFor = Exception.class)
    void updateCurrentUserPassword(Long userId, UserPasswordUpdateDTO dto);

    boolean usernameExists(Long userId, String username);

    boolean emailExists(Long userId, String email);
}
