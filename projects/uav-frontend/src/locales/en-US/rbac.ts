const rbac = {
  title: "Role & Permission Management",
  subtitle: "Admins can assign roles to users and permissions to roles",
  refresh: "Refresh",
  users: {
    title: "User Role Assignment",
    search: "Username / Nickname",
    searchPlaceholder: "Search by keyword",
    query: "Search",
    reset: "Reset",
    columns: {
      username: "Username",
      nickname: "Nickname",
      roles: "Roles",
      actions: "Actions",
    },
    configureRoles: "Configure Roles",
    noRoles: "No roles",
    saveSuccess: "User roles updated",
  },
  roles: {
    title: "Role Permission Assignment",
    search: "Role name / code",
    searchPlaceholder: "Filter roles by keyword",
    create: "New Role",
    createSuccess: "Role created",
    columns: {
      code: "Role Code",
      name: "Role Name",
      permissions: "Permissions",
      actions: "Actions",
    },
    configurePermissions: "Configure Permissions",
    noPermissions: "No permissions",
    saveSuccess: "Role permissions updated",
  },
  dialogs: {
    userRoleTitle: "Configure User Roles",
    rolePermissionTitle: "Configure Role Permissions",
    createRoleTitle: "Create Role",
    selectRoles: "Select Roles",
    selectPermissions: "Select Permissions",
    roleCode: "Role Code",
    roleName: "Role Name",
    roleDescription: "Role Description",
    roleCodeHint:
      "Format: lowercase letters, digits, colon. Example: rbac:role:create",
    roleCodeRule:
      "Role code allows lowercase letters, digits, and colon only, without leading/trailing colon",
    defaultPermissions: "Default Permissions (Optional)",
    defaultPermissionsHint:
      "Pre-select initial permissions and bind them right after role creation",
    quickPickAllPermissions: "Select All",
    clearPickedPermissions: "Clear",
    cancel: "Cancel",
    save: "Save",
  },
  flow: {
    step1Title: "Step 1: Build Role Pool",
    step1Desc:
      "Create roles first and align code conventions before mapping permissions.",
    step2Title: "Step 2: Pick Permission Catalog",
    step2Desc: "Review assignable permission codes to keep mappings accurate.",
  },
  readonly: {
    title: "Read-Only Mode",
    desc: "You can view RBAC data but cannot change it. Contact an administrator for write permissions.",
  },
  common: {
    loadingFailed: "Failed to load data",
    saveFailed: "Save failed, please try again",
    noPermissionAction: "Your account does not have permission for this action",
    empty: "No data",
  },
};

export default rbac;
