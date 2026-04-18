import { request } from "@/shared/http/request";
import type {
  PagedResponse,
  RbacPermission,
  RbacRole,
  RbacUser,
  RbacUserPageRequest,
  RoleCreateRequest,
  RolePermissionUpdateRequest,
  UserRoleUpdateRequest,
} from "@/types";

export async function fetchRbacUserPage(
  query: RbacUserPageRequest,
): Promise<PagedResponse<RbacUser>> {
  return request.get<PagedResponse<RbacUser>>("/admin/rbac/users/page", {
    params: query,
  });
}

export async function fetchRbacRoles(): Promise<RbacRole[]> {
  return request.get<RbacRole[]>("/admin/rbac/roles");
}

export async function createRbacRole(
  payload: RoleCreateRequest,
): Promise<void> {
  await request.post<void>("/admin/rbac/roles", payload);
}

export async function fetchRbacPermissions(): Promise<RbacPermission[]> {
  return request.get<RbacPermission[]>("/admin/rbac/permissions");
}

export async function updateUserRoles(
  userId: number,
  payload: UserRoleUpdateRequest,
): Promise<void> {
  await request.put<void>(`/admin/rbac/users/${userId}/roles`, payload);
}

export async function updateRolePermissions(
  roleId: number,
  payload: RolePermissionUpdateRequest,
): Promise<void> {
  await request.put<void>(`/admin/rbac/roles/${roleId}/permissions`, payload);
}
