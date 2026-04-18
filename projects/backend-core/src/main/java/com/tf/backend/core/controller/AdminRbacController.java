package com.tf.backend.core.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tf.backend.core.application.domain.rbac.AdminRbacCommandService;
import com.tf.backend.core.application.domain.rbac.AdminRbacQueryService;
import com.tf.backend.core.common.response.R;
import com.tf.backend.core.model.dto.RoleCreateDTO;
import com.tf.backend.core.model.dto.RolePermissionUpdateDTO;
import com.tf.backend.core.model.dto.UserRoleUpdateDTO;
import com.tf.backend.core.model.vo.AdminRbacPermissionVO;
import com.tf.backend.core.model.vo.AdminRbacRoleVO;
import com.tf.backend.core.model.vo.AdminRbacUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/rbac")
@PreAuthorize("hasRole('admin')")
@Tag(name = "管理员RBAC", description = "仅管理员可用：用户角色与角色权限管理")
public class AdminRbacController {

    private final AdminRbacQueryService adminRbacQueryService;

    private final AdminRbacCommandService adminRbacCommandService;


    @GetMapping("/users/page")
    @Operation(summary = "分页查询用户及角色")
    @PreAuthorize("hasAuthority('rbac:user:page')")
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
        return R.ok(adminRbacQueryService.getUserPage(current, size, keyword));
    }


    @GetMapping("/roles")
    @Operation(summary = "查询全部角色及权限")
    @PreAuthorize("hasAuthority('rbac:role:list')")
    public R<List<AdminRbacRoleVO>> getRoles() {
        return R.ok(adminRbacQueryService.getRoles());
    }


    @PostMapping("/roles")
    @Operation(summary = "新增角色")
    @PreAuthorize("hasAuthority('rbac:role:create')")
    public R<Void> createRole(

            @Valid
            @RequestBody
            RoleCreateDTO dto

    ) {
        adminRbacCommandService.createRole(dto);

        return R.okWithMsg("角色新增成功");
    }


    @GetMapping("/permissions")
    @Operation(summary = "查询全部权限")
    @PreAuthorize("hasAuthority('rbac:permission:list')")
    public R<List<AdminRbacPermissionVO>> getPermissions() {
        return R.ok(adminRbacQueryService.getPermissions());
    }


    @PutMapping("/users/{userId}/roles")
    @Operation(summary = "更新用户角色集合")
    @PreAuthorize("hasAuthority('rbac:user:role:update')")
    public R<Void> updateUserRoles(

            @Parameter(description = "用户ID", required = true)
            @PathVariable
            Long userId,

            @Valid
            @RequestBody
            UserRoleUpdateDTO dto

    ) {
        adminRbacCommandService.updateUserRoles(userId, dto);

        return R.okWithMsg("用户角色更新成功");
    }


    @PutMapping("/roles/{roleId}/permissions")
    @Operation(summary = "更新角色权限集合")
    @PreAuthorize("hasAuthority('rbac:role:permission:update')")
    public R<Void> updateRolePermissions(

            @Parameter(description = "角色ID", required = true)
            @PathVariable
            Long roleId,

            @Valid
            @RequestBody
            RolePermissionUpdateDTO dto

    ) {
        adminRbacCommandService.updateRolePermissions(roleId, dto);

        return R.okWithMsg("角色权限更新成功");
    }

    
}
