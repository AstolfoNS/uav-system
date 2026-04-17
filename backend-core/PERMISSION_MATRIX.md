# 后端权限矩阵（RBAC）

本文档基于当前后端代码与数据库初始化脚本整理，用于说明：

- 哪些接口是公共接口
- 哪些接口需要权限
- 每个接口对应的权限码
- 默认角色 admin 已覆盖哪些权限

## 1. 公共接口（无需登录）

说明：根据 SecurityConfig 的放行规则，/auth/\*\* 为公开接口。

| 模块 | 方法 | 路径          | 权限码                     |
| ---- | ---- | ------------- | -------------------------- |
| 认证 | POST | /auth/login   | 无                         |
| 认证 | POST | /auth/refresh | 无                         |
| 认证 | POST | /auth/logout  | 无（但通常需要携带登录态） |

## 2. 用户与文件模块

| 模块 | 方法 | 路径                 | 权限码              |
| ---- | ---- | -------------------- | ------------------- |
| 用户 | GET  | /users/profile       | user:profile:view   |
| 用户 | PUT  | /users/profile       | user:profile:update |
| 文件 | POST | /files/upload        | file:upload:common  |
| 文件 | POST | /files/upload/avatar | file:upload:avatar  |

## 3. 模型节点模块

| 模块     | 方法   | 路径                  | 权限码            |
| -------- | ------ | --------------------- | ----------------- |
| 模型节点 | GET    | /yolo-nodes/page      | model:node:page   |
| 模型节点 | GET    | /yolo-nodes/{id}      | model:node:detail |
| 模型节点 | POST   | /yolo-nodes           | model:node:create |
| 模型节点 | PUT    | /yolo-nodes/{id}      | model:node:update |
| 模型节点 | DELETE | /yolo-nodes/{id}      | model:node:delete |
| 模型节点 | POST   | /yolo-nodes/{id}/sync | model:node:sync   |

### 3.1 参数模板子模块

| 模块     | 方法   | 路径                                                  | 权限码             |
| -------- | ------ | ----------------------------------------------------- | ------------------ |
| 参数模板 | PUT    | /yolo-nodes/{id}/params/template/{templateName}/apply | model:param:apply  |
| 参数模板 | GET    | /yolo-nodes/{id}/params/templates                     | model:param:list   |
| 参数模板 | GET    | /yolo-nodes/{id}/params/templates/{templateName}      | model:param:detail |
| 参数模板 | POST   | /yolo-nodes/{id}/params/templates                     | model:param:create |
| 参数模板 | PUT    | /yolo-nodes/{id}/params/templates/{templateName}      | model:param:update |
| 参数模板 | DELETE | /yolo-nodes/{id}/params/templates/{templateName}      | model:param:delete |

### 3.2 模型权重子模块

| 模块 | 方法   | 路径                                       | 权限码              |
| ---- | ------ | ------------------------------------------ | ------------------- |
| 权重 | GET    | /yolo-nodes/{id}/weights                   | model:weight:list   |
| 权重 | POST   | /yolo-nodes/{id}/weights                   | model:weight:upload |
| 权重 | PUT    | /yolo-nodes/{id}/weights/{filename}/active | model:weight:switch |
| 权重 | DELETE | /yolo-nodes/{id}/weights/{filename}        | model:weight:delete |

## 4. 推理与记录模块

| 模块 | 方法   | 路径                            | 权限码                  |
| ---- | ------ | ------------------------------- | ----------------------- |
| 推理 | POST   | /inference/nodes/{nodeId}/image | inference:image:run     |
| 推理 | POST   | /inference/nodes/{nodeId}/video | inference:video:run     |
| 记录 | GET    | /inference/records/page         | inference:record:page   |
| 记录 | GET    | /inference/records/{id}         | inference:record:detail |
| 记录 | DELETE | /inference/records/{id}         | inference:record:delete |

## 5. 默认角色覆盖关系

当前数据库初始化脚本已配置：

- 角色：admin
- 用户：TimeLeafing
- 绑定关系：TimeLeafing -> admin
- admin -> 全部 API 权限码（上表所有需要权限的接口）

## 6. SQL 对应关系（简要）

初始化脚本中涉及：

- 角色表：roles
- 权限表：permissions
- 用户角色关系：user_roles
- 角色权限关系：role_permissions

所有插入语句均采用幂等写法（基于 not exists），可重复执行。

## 7. 后续建议

- 若要新增运营角色（如 operator、viewer），建议按以下策略拆分：
  - viewer：只读（list/detail/page）
  - operator：可执行推理、上传文件、同步节点
  - admin：全量读写删

- 角色拆分完成后，建议补一份前端菜单可见性矩阵（菜单项 -> 权限码）。
