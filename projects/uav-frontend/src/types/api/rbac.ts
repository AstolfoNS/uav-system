export interface RbacUserPageRequest {
  current: number;
  size: number;
  keyword?: string;
}

export interface RbacUser {
  id: number;
  username: string;
  nickname?: string;
  roleIds: number[];
  roles: string[];
}

export interface RbacRole {
  id: number;
  code: string;
  name?: string;
  description?: string;
  level?: number;
  sortOrder?: number;
  permissionIds: number[];
  permissions: string[];
}

export interface RbacPermission {
  id: number;
  code: string;
  name?: string;
  type?: number | null;
  description?: string;
  sortOrder?: number;
}

export interface UserRoleUpdateRequest {
  roleIds: number[];
}

export interface RolePermissionUpdateRequest {
  permissionIds: number[];
}

export interface RoleCreateRequest {
  code: string;
  name: string;
  description?: string;
  level?: number;
  sortOrder?: number;
  permissionIds?: number[];
}
