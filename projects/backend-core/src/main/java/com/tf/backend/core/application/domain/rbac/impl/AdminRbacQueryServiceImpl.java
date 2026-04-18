package com.tf.backend.core.application.domain.rbac.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tf.backend.core.application.domain.rbac.AdminRbacQueryService;
import com.tf.backend.core.application.infrastructure.repo.PermissionService;
import com.tf.backend.core.application.infrastructure.repo.RolePermissionService;
import com.tf.backend.core.application.infrastructure.repo.RoleService;
import com.tf.backend.core.application.infrastructure.repo.UserRoleService;
import com.tf.backend.core.application.infrastructure.repo.UserService;
import com.tf.backend.core.model.entity.PermissionEntity;
import com.tf.backend.core.model.entity.RoleEntity;
import com.tf.backend.core.model.entity.RolePermissionEntity;
import com.tf.backend.core.model.entity.UserEntity;
import com.tf.backend.core.model.entity.UserRoleEntity;
import com.tf.backend.core.model.vo.AdminRbacPermissionVO;
import com.tf.backend.core.model.vo.AdminRbacRoleVO;
import com.tf.backend.core.model.vo.AdminRbacUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminRbacQueryServiceImpl implements AdminRbacQueryService {

    private final UserService userService;

    private final RoleService roleService;

    private final PermissionService permissionService;

    private final UserRoleService userRoleService;

    private final RolePermissionService rolePermissionService;

    @Override
    public IPage<AdminRbacUserVO> getUserPage(Integer current, Integer size, String keyword) {
        Page<UserEntity> page = new Page<>(current, size);
        IPage<UserEntity> userPage = userService.page(
                page,
                Wrappers.<UserEntity>lambdaQuery()
                        .and(
                                StringUtils.hasText(keyword),
                                wrapper -> wrapper.like(UserEntity::getUsername, keyword)
                                        .or()
                                        .like(UserEntity::getNickname, keyword)
                        )
                        .orderByAsc(UserEntity::getId)
        );

        List<UserEntity> users = userPage.getRecords();
        List<Long> userIds = users.stream().map(UserEntity::getId).toList();

        Map<Long, List<UserRoleEntity>> userRoleMap = userRoleService.list(
                Wrappers.<UserRoleEntity>lambdaQuery().in(!userIds.isEmpty(), UserRoleEntity::getUserId, userIds)
        ).stream().collect(Collectors.groupingBy(UserRoleEntity::getUserId));

        Set<Long> roleIds = userRoleMap.values().stream()
                .flatMap(Collection::stream)
                .map(UserRoleEntity::getRoleId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, RoleEntity> roleMap = roleService.listByIds(roleIds).stream()
                .collect(Collectors.toMap(RoleEntity::getId, Function.identity()));

        List<AdminRbacUserVO> records = new ArrayList<>();
        for (UserEntity user : users) {
            List<UserRoleEntity> relations = userRoleMap.getOrDefault(user.getId(), List.of());
            List<Long> roleIdList = relations.stream().map(UserRoleEntity::getRoleId).distinct().toList();
            List<String> roleCodes = roleIdList.stream()
                    .map(roleMap::get)
                    .filter(java.util.Objects::nonNull)
                    .map(RoleEntity::getCode)
                    .toList();

            records.add(new AdminRbacUserVO(
                    user.getId(),
                    user.getUsername(),
                    user.getNickname(),
                    roleIdList,
                    roleCodes
            ));
        }

        Page<AdminRbacUserVO> mappedPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        mappedPage.setRecords(records);
        return mappedPage;
    }

    @Override
    public List<AdminRbacRoleVO> getRoles() {
        List<RoleEntity> roles = roleService.list(
                Wrappers.<RoleEntity>lambdaQuery().orderByAsc(RoleEntity::getSortOrder).orderByAsc(RoleEntity::getId)
        );

        List<Long> roleIds = roles.stream().map(RoleEntity::getId).toList();

        Map<Long, List<RolePermissionEntity>> relationMap = rolePermissionService.list(
                Wrappers.<RolePermissionEntity>lambdaQuery().in(!roleIds.isEmpty(), RolePermissionEntity::getRoleId, roleIds)
        ).stream().collect(Collectors.groupingBy(RolePermissionEntity::getRoleId));

        Set<Long> permissionIds = relationMap.values().stream()
                .flatMap(Collection::stream)
                .map(RolePermissionEntity::getPermissionId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, PermissionEntity> permissionMap = permissionService.listByIds(permissionIds).stream()
                .collect(Collectors.toMap(PermissionEntity::getId, Function.identity()));

        List<AdminRbacRoleVO> result = new ArrayList<>();
        for (RoleEntity role : roles) {
            List<Long> assignedPermissionIds = relationMap.getOrDefault(role.getId(), List.of())
                    .stream()
                    .map(RolePermissionEntity::getPermissionId)
                    .distinct()
                    .toList();
            List<String> permissionCodes = assignedPermissionIds.stream()
                    .map(permissionMap::get)
                    .filter(java.util.Objects::nonNull)
                    .map(PermissionEntity::getCode)
                    .toList();

            result.add(new AdminRbacRoleVO(
                    role.getId(),
                    role.getCode(),
                    role.getName(),
                    role.getDescription(),
                    role.getLevel(),
                    role.getSortOrder(),
                    assignedPermissionIds,
                    permissionCodes
            ));
        }

        return result;
    }

    @Override
    public List<AdminRbacPermissionVO> getPermissions() {
        List<PermissionEntity> permissions = permissionService.list(
                Wrappers.<PermissionEntity>lambdaQuery()
                        .orderByAsc(PermissionEntity::getSortOrder)
                        .orderByAsc(PermissionEntity::getId)
        );

        return permissions.stream()
                .map(permission -> new AdminRbacPermissionVO(
                        permission.getId(),
                        permission.getCode(),
                        permission.getName(),
                        permission.getType() == null ? null : permission.getType().getCode(),
                        permission.getDescription(),
                        permission.getSortOrder()
                ))
                .toList();
    }
}
