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
    selectRoles: "Select Roles",
    selectPermissions: "Select Permissions",
    cancel: "Cancel",
    save: "Save",
  },
  common: {
    loadingFailed: "Failed to load data",
    saveFailed: "Save failed, please try again",
    empty: "No data",
  },
};

export default rbac;
