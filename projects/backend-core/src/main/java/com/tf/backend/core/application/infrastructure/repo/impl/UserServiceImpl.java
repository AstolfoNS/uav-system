package com.tf.backend.core.application.infrastructure.repo.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tf.backend.core.application.domain.file.SysFileService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tf.backend.core.application.mapper.UserMapper;
import com.tf.backend.core.common.exception.BizException;
import com.tf.backend.core.model.dto.UserPasswordUpdateDTO;
import com.tf.backend.core.model.dto.UserProfileUpdateDTO;
import com.tf.backend.core.model.entity.UserEntity;
import com.tf.backend.core.application.infrastructure.repo.UserService;
import com.tf.backend.core.model.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    private final SysFileService sysFileService;

    private final PasswordEncoder passwordEncoder;

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


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateUserProfile(Long userId, UserProfileUpdateDTO dto) {
        UserEntity currentUser = this.getOptById(userId).orElseThrow(
                () -> new BizException("用户不存在")
        );

        if (StringUtils.hasText(dto.username()) && !dto.username().equals(currentUser.getUsername())) {
            if (usernameExists(userId, dto.username())) {
                throw new BizException("用户名已存在，请更换后重试");
            }
        }

        // 使用 dto.email() 替代 dto.getEmail()
        if (StringUtils.hasText(dto.email()) && !dto.email().equals(currentUser.getEmail())) {
            if (emailExists(userId, dto.email())) {
                throw new BizException("该邮箱已被其他账号绑定");
            }
        }

        // 使用 dto.phoneNumber() 替代 dto.getPhoneNumber()
        if (StringUtils.hasText(dto.phoneNumber()) && !dto.phoneNumber().equals(currentUser.getPhoneNumber())) {
            if (phoneNumberExists(userId, dto.phoneNumber())) {
                throw new BizException("该手机号已被其他账号绑定");
            }
        }

        String oldAvatarUrl = currentUser.getAvatarUrl();
        String newAvatarUrl = dto.avatarUrl();

        UserEntity updateEntity = UserEntity.builder()
                .id(userId)
                .username(dto.username())
                .nickname(dto.nickname())
                .email(dto.email())
                .phoneNumber(dto.phoneNumber())
                .avatarUrl(dto.avatarUrl())
                .gender(dto.gender())
                .introduction(dto.introduction())
                .build();

        this.updateById(updateEntity);

        if (StringUtils.hasText(oldAvatarUrl) && !oldAvatarUrl.equals(newAvatarUrl)) {
            sysFileService.deleteFileByUrl(oldAvatarUrl);
        }
    }


    @Override
    public UserProfileVO getCurrentUserProfile(Long userId, List<String> roles, List<String> permissions) {
        UserEntity user = this.getOptById(userId).orElseThrow(
                () -> new BizException("用户不存在")
        );

        return UserProfileVO.mapToVO(user, roles == null ? List.of() : roles, permissions == null ? List.of() : permissions);
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserProfileVO updateCurrentUserProfile(Long userId, List<String> roles, List<String> permissions, UserProfileUpdateDTO dto) {
        updateUserProfile(userId, dto);

        return getCurrentUserProfile(userId, roles, permissions);
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateCurrentUserPassword(Long userId, UserPasswordUpdateDTO dto) {
        UserEntity currentUser = this.getOptById(userId).orElseThrow(
                () -> new BizException("用户不存在")
        );

        if (!passwordEncoder.matches(dto.currentPassword(), currentUser.getPassword())) {
            throw new BizException("当前密码不正确");
        }

        if (!dto.newPassword().equals(dto.confirmPassword())) {
            throw new BizException("两次输入的新密码不一致");
        }

        if (dto.newPassword().equals(dto.currentPassword())) {
            throw new BizException("新密码不能与当前密码相同");
        }

        UserEntity updateEntity = new UserEntity();
        updateEntity.setId(userId);
        updateEntity.setPassword(passwordEncoder.encode(dto.newPassword()));

        this.updateById(updateEntity);
    }


    @Override
    public boolean usernameExists(Long userId, String username) {
        return this.count(
                Wrappers.<UserEntity>lambdaQuery()
                        .eq(UserEntity::getUsername, username)
                        .ne(UserEntity::getId, userId)
        ) > 0;
    }


    @Override
    public boolean emailExists(Long userId, String email) {
        return this.count(
                Wrappers.<UserEntity>lambdaQuery()
                        .eq(UserEntity::getEmail, email)
                        .ne(UserEntity::getId, userId)
        ) > 0;
    }


    private boolean phoneNumberExists(Long userId, String phoneNumber) {
        return this.count(
                Wrappers.<UserEntity>lambdaQuery()
                        .eq(UserEntity::getPhoneNumber, phoneNumber)
                        .ne(UserEntity::getId, userId)
        ) > 0;
    }
}
