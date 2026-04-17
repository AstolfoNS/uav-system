package com.tf.backend.core.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tf.backend.core.application.domain.auth.AuthenticationService;
import com.tf.backend.core.application.infrastructure.repo.PermissionService;
import com.tf.backend.core.application.infrastructure.repo.RolePermissionService;
import com.tf.backend.core.application.infrastructure.repo.RoleService;
import com.tf.backend.core.application.infrastructure.repo.UserRoleService;
import com.tf.backend.core.application.infrastructure.repo.UserService;
import com.tf.backend.core.common.exception.BizException;
import com.tf.backend.core.common.response.R;
import com.tf.backend.core.model.dto.RolePermissionUpdateDTO;
import com.tf.backend.core.model.dto.UserRoleUpdateDTO;
import com.tf.backend.core.model.entity.PermissionEntity;
import com.tf.backend.core.model.entity.RoleEntity;
import com.tf.backend.core.model.entity.RolePermissionEntity;
import com.tf.backend.core.model.entity.UserEntity;
import com.tf.backend.core.model.entity.UserRoleEntity;
import com.tf.backend.core.model.vo.AdminRbacPermissionVO;
import com.tf.backend.core.model.vo.AdminRbacRoleVO;
import com.tf.backend.core.model.vo.AdminRbacUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/rbac")
@PreAuthorize("hasRole('admin')")
@Tag(name = "管理员RBAC", description = "仅管理员可用：用户角色与角色权限管理")
public class AdminRbacController {

    private final UserService userService;

    private final RoleService roleService;

    private final PermissionService permissionService;

    private final UserRoleService userRoleService;

    private final RolePermissionService rolePermissionService;

    private final AuthenticationService authenticationService;

    @GetMapping("/users/page")
    @Operation(summary = "分页查询用户及角色")
    public R<IPage<AdminRbacUserVO>> getUserPage(

            @Parameter(description = "当前页码")
            @RequestParam(defaultValue = "1")
            Integer current,

            @Parameter(description = "每页大小")
            @RequestParam(defaultValue = "10")
            Integer size,

            @Parameter(description = "用户名/昵称关键字")
            @RequestParam(required = false)
            String keyword

    ) {
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
        return R.ok(mappedPage);
    }

    @GetMapping("/roles")
    @Operation(summary = "查询全部角色及权限")
    public R<List<AdminRbacRoleVO>> getRoles() {
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

        return R.ok(result);
    }

    @GetMapping("/permissions")
    @Operation(summary = "查询全部权限")
    public R<List<AdminRbacPermissionVO>> getPermissions() {
        List<PermissionEntity> permissions = permissionService.list(
                Wrappers.<PermissionEntity>lambdaQuery()
                        .orderByAsc(PermissionEntity::getSortOrder)
                        .orderByAsc(PermissionEntity::getId)
        );

        List<AdminRbacPermissionVO> result = permissions.stream()
                .map(permission -> new AdminRbacPermissionVO(
                        permission.getId(),
                        permission.getCode(),
                        permission.getName(),
                        permission.getType() == null ? null : permission.getType().getCode(),
                        permission.getDescription(),
                        permission.getSortOrder()
                ))
                .toList();

        return R.ok(result);
    }

    @PutMapping("/users/{userId}/roles")
    @Operation(summary = "更新用户角色集合")
    public R<Void> updateUserRoles(

            @Parameter(description = "用户ID", required = true)
            @PathVariable
            Long userId,

            @Valid
            @RequestBody
            UserRoleUpdateDTO dto

    ) {
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

        Set<Long> targetRoleIdSet = new LinkedHashSet<>(targetRoleIds);

        Set<Long> toRemove = new LinkedHashSet<>(currentRoleIds);
        toRemove.removeAll(targetRoleIdSet);

        Set<Long> toAdd = new LinkedHashSet<>(targetRoleIdSet);
        toAdd.removeAll(currentRoleIds);

        if (!toRemove.isEmpty()) {
            userRoleService.remove(
                    Wrappers.<UserRoleEntity>lambdaQuery()
                            .eq(UserRoleEntity::getUserId, userId)
                            .in(UserRoleEntity::getRoleId, toRemove)
            );
        }

        if (!toAdd.isEmpty()) {
                        List<UserRoleEntity> addList = new ArrayList<>();
                        for (Long newRoleId : toAdd) {
                                addList.add(UserRoleEntity.builder().userId(userId).roleId(newRoleId).build());
                        }
            userRoleService.saveBatch(addList);
        }

        authenticationService.evictLoginUserCache(userId);
        return R.okWithMsg("用户角色更新成功");
    }

    @PutMapping("/roles/{roleId}/permissions")
    @Operation(summary = "更新角色权限集合")
    public R<Void> updateRolePermissions(

            @Parameter(description = "角色ID", required = true)
            @PathVariable
            Long roleId,

            @Valid
            @RequestBody
            RolePermissionUpdateDTO dto

    ) {
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

        Set<Long> targetPermissionIdSet = new LinkedHashSet<>(targetPermissionIds);

        Set<Long> toRemove = new LinkedHashSet<>(currentPermissionIds);
        toRemove.removeAll(targetPermissionIdSet);

        Set<Long> toAdd = new LinkedHashSet<>(targetPermissionIdSet);
        toAdd.removeAll(currentPermissionIds);

        if (!toRemove.isEmpty()) {
            rolePermissionService.remove(
                    Wrappers.<RolePermissionEntity>lambdaQuery()
                            .eq(RolePermissionEntity::getRoleId, roleId)
                            .in(RolePermissionEntity::getPermissionId, toRemove)
            );
        }

        if (!toAdd.isEmpty()) {
                        List<RolePermissionEntity> addList = new ArrayList<>();
                        for (Long newPermissionId : toAdd) {
                                addList.add(RolePermissionEntity.builder().roleId(roleId).permissionId(newPermissionId).build());
                        }
            rolePermissionService.saveBatch(addList);
        }

        List<Long> affectedUserIds = userRoleService.list(
                Wrappers.<UserRoleEntity>lambdaQuery().eq(UserRoleEntity::getRoleId, roleId)
        ).stream().map(UserRoleEntity::getUserId).distinct().toList();

        affectedUserIds.forEach(authenticationService::evictLoginUserCache);
        return R.okWithMsg("角色权限更新成功");
    }

    private List<Long> sanitizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
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
}
