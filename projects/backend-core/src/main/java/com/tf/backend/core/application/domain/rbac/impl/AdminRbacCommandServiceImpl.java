package com.tf.backend.core.application.domain.rbac.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tf.backend.core.application.domain.auth.AuthenticationService;
import com.tf.backend.core.application.domain.rbac.AdminRbacCommandService;
import com.tf.backend.core.application.infrastructure.repo.PermissionService;
import com.tf.backend.core.application.infrastructure.repo.RolePermissionService;
import com.tf.backend.core.application.infrastructure.repo.RoleService;
import com.tf.backend.core.application.infrastructure.repo.UserRoleService;
import com.tf.backend.core.application.infrastructure.repo.UserService;
import com.tf.backend.core.common.exception.BizException;
import com.tf.backend.core.model.dto.RoleCreateDTO;
import com.tf.backend.core.model.dto.RolePermissionUpdateDTO;
import com.tf.backend.core.model.dto.UserRoleUpdateDTO;
import com.tf.backend.core.model.entity.RoleEntity;
import com.tf.backend.core.model.entity.PermissionEntity;
import com.tf.backend.core.model.entity.RolePermissionEntity;
import com.tf.backend.core.model.entity.UserEntity;
import com.tf.backend.core.model.entity.UserRoleEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminRbacCommandServiceImpl implements AdminRbacCommandService {

    private final UserService userService;

    private final RoleService roleService;

    private final PermissionService permissionService;

    private final UserRoleService userRoleService;

    private final RolePermissionService rolePermissionService;

    private final AuthenticationService authenticationService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createRole(RoleCreateDTO dto) {
        String code = normalizeRequired(dto.code(), "角色编码不能为空");
        String name = normalizeRequired(dto.name(), "角色名称不能为空");
        String description = normalizeOptional(dto.description());
        Integer level = dto.level() == null ? 0 : dto.level();
        Integer sortOrder = dto.sortOrder() == null ? 0 : dto.sortOrder();
        List<Long> targetPermissionIds = sanitizeIds(dto.permissionIds());
        ensureAllPermissionsExist(targetPermissionIds);

        boolean codeExists = roleService.lambdaQuery()
                .eq(RoleEntity::getCode, code)
                .exists();
        if (codeExists) {
            throw new BizException("角色编码已存在");
        }

        RoleEntity role = RoleEntity.builder()
                .code(code)
                .name(name)
                .description(description)
                .level(level)
                .sortOrder(sortOrder)
                .build();

        roleService.save(role);

            if (!targetPermissionIds.isEmpty()) {
                rolePermissionService.saveBatch(
                    buildRolePermissionRelations(role.getId(), new LinkedHashSet<>(targetPermissionIds))
                );
            }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateUserRoles(Long userId, UserRoleUpdateDTO dto) {
        if (!userService.lambdaQuery().eq(UserEntity::getId, userId).exists()) {
            throw new BizException("目标用户不存在");
        }

        List<Long> targetRoleIds = sanitizeIds(dto.roleIds());
        ensureAllRolesExist(targetRoleIds);

        List<UserRoleEntity> currentRelations = userRoleService.list(
                Wrappers.<UserRoleEntity>lambdaQuery().eq(UserRoleEntity::getUserId, userId)
        );

        Set<Long> currentRoleIds = currentRelations.stream()
                .map(UserRoleEntity::getRoleId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        AssignmentDiff roleDiff = calculateAssignmentDiff(currentRoleIds, targetRoleIds);
        applyAssignmentDiff(
            roleDiff,
            toRemove -> userRoleService.remove(
                Wrappers.<UserRoleEntity>lambdaQuery()
                    .eq(UserRoleEntity::getUserId, userId)
                    .in(UserRoleEntity::getRoleId, toRemove)
            ),
            toAdd -> userRoleService.saveBatch(buildUserRoleRelations(userId, toAdd))
        );

        authenticationService.evictLoginUserCache(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateRolePermissions(Long roleId, RolePermissionUpdateDTO dto) {
        if (!roleService.lambdaQuery().eq(RoleEntity::getId, roleId).exists()) {
            throw new BizException("目标角色不存在");
        }

        List<Long> targetPermissionIds = sanitizeIds(dto.permissionIds());
        ensureAllPermissionsExist(targetPermissionIds);

        List<RolePermissionEntity> currentRelations = rolePermissionService.list(
                Wrappers.<RolePermissionEntity>lambdaQuery().eq(RolePermissionEntity::getRoleId, roleId)
        );

        Set<Long> currentPermissionIds = currentRelations.stream()
                .map(RolePermissionEntity::getPermissionId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        AssignmentDiff permissionDiff = calculateAssignmentDiff(currentPermissionIds, targetPermissionIds);
        applyAssignmentDiff(
            permissionDiff,
            toRemove -> rolePermissionService.remove(
                Wrappers.<RolePermissionEntity>lambdaQuery()
                    .eq(RolePermissionEntity::getRoleId, roleId)
                    .in(RolePermissionEntity::getPermissionId, toRemove)
            ),
            toAdd -> rolePermissionService.saveBatch(buildRolePermissionRelations(roleId, toAdd))
        );

        List<Long> affectedUserIds = userRoleService.list(
                Wrappers.<UserRoleEntity>lambdaQuery().eq(UserRoleEntity::getRoleId, roleId)
        ).stream().map(UserRoleEntity::getUserId).distinct().toList();

        affectedUserIds.forEach(authenticationService::evictLoginUserCache);
    }

    private List<Long> sanitizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private String normalizeRequired(String raw, String message) {
        String value = normalizeOptional(raw);
        if (value == null) {
            throw new BizException(message);
        }
        return value;
    }

    private String normalizeOptional(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return value.isEmpty() ? null : value;
    }

    private void ensureAllRolesExist(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return;
        }

        long count = roleService.lambdaQuery().in(RoleEntity::getId, roleIds).count();
        if (count != roleIds.size()) {
            throw new BizException("存在无效角色ID");
        }
    }

    private void ensureAllPermissionsExist(List<Long> permissionIds) {
        if (permissionIds.isEmpty()) {
            return;
        }

        long count = permissionService.lambdaQuery().in(PermissionEntity::getId, permissionIds).count();
        if (count != permissionIds.size()) {
            throw new BizException("存在无效权限ID");
        }
    }

    private AssignmentDiff calculateAssignmentDiff(Set<Long> currentIds, List<Long> targetIds) {
        Set<Long> targetIdSet = new LinkedHashSet<>(targetIds);

        Set<Long> toRemove = new LinkedHashSet<>(currentIds);
        toRemove.removeAll(targetIdSet);

        Set<Long> toAdd = new LinkedHashSet<>(targetIdSet);
        toAdd.removeAll(currentIds);

        return new AssignmentDiff(toAdd, toRemove);
    }

    private void applyAssignmentDiff(
            AssignmentDiff diff,
            Consumer<Set<Long>> removeAction,
            Consumer<Set<Long>> addAction
    ) {
        if (!diff.toRemove().isEmpty()) {
            removeAction.accept(diff.toRemove());
        }

        if (!diff.toAdd().isEmpty()) {
            addAction.accept(diff.toAdd());
        }
    }

    private List<UserRoleEntity> buildUserRoleRelations(Long userId, Set<Long> roleIds) {
        List<UserRoleEntity> addList = new ArrayList<>();
        for (Long roleId : roleIds) {
            addList.add(UserRoleEntity.builder().userId(userId).roleId(roleId).build());
        }
        return addList;
    }

    private List<RolePermissionEntity> buildRolePermissionRelations(Long roleId, Set<Long> permissionIds) {
        List<RolePermissionEntity> addList = new ArrayList<>();
        for (Long permissionId : permissionIds) {
            addList.add(RolePermissionEntity.builder().roleId(roleId).permissionId(permissionId).build());
        }
        return addList;
    }

    private record AssignmentDiff(Set<Long> toAdd, Set<Long> toRemove) {
    }
}
