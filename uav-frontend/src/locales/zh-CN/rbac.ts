const rbac = {
  title: "角色与权限管理",
  subtitle: "管理员可为用户配置角色，并为角色配置权限",
  refresh: "刷新",
  users: {
    title: "用户角色分配",
    search: "用户名/昵称",
    searchPlaceholder: "输入关键字后查询",
    query: "查询",
    reset: "重置",
    columns: {
      username: "用户名",
      nickname: "昵称",
      roles: "角色",
      actions: "操作",
    },
    configureRoles: "配置角色",
    noRoles: "暂无角色",
    saveSuccess: "用户角色更新成功",
  },
  roles: {
    title: "角色权限分配",
    search: "角色名称/编码",
    searchPlaceholder: "输入关键字筛选角色",
    columns: {
      code: "角色编码",
      name: "角色名称",
      permissions: "权限",
      actions: "操作",
    },
    configurePermissions: "配置权限",
    noPermissions: "暂无权限",
    saveSuccess: "角色权限更新成功",
  },
  dialogs: {
    userRoleTitle: "配置用户角色",
    rolePermissionTitle: "配置角色权限",
    selectRoles: "选择角色",
    selectPermissions: "选择权限",
    cancel: "取消",
    save: "保存",
  },
  common: {
    loadingFailed: "数据加载失败",
    saveFailed: "保存失败，请稍后重试",
    empty: "暂无数据",
  },
};

export default rbac;
