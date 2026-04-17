# UAV System Backend API 文档

本文档对应 `backend-core`（Spring Boot）服务当前实现。

## 1. 基础信息

- 服务默认地址: `http://<host>:8111`
- 全局前缀（context-path）: `/api/v1`
- 接口完整前缀: `http://<host>:8111/api/v1`
- 文档调试入口: `/swagger-ui/index.html`（完整路径示例: `http://<host>:8111/api/v1/swagger-ui/index.html`）

## 2. 鉴权与请求头

- 除 `/auth/**`、`/swagger-ui/**`、`/actuator/**`、`/public/**` 外，其余接口默认需要 Access Token。
- Access Token 头:
  - `Authorization: Bearer <access_token>`
- Refresh Token 头（用于刷新/登出相关逻辑）:
  - `Refresh-Token: Bearer <refresh_token>`

## 3. 统一响应结构

所有 Controller 统一返回 `R<T>`，结构如下：

```json
{
  "code": 200,
  "msg": "success",
  "data": {},
  "details": null
}
```

字段说明：

- `code`: 业务状态码
- `msg`: 业务提示信息
- `data`: 业务数据
- `details`: 错误详情（可为空）

## 4. 认证模块 Auth

### 4.1 登录

- Method: `POST`
- URL: `/api/v1/auth/login`
- Auth: 否
- Content-Type: `application/json`
- Body:
  - `username` string 必填
  - `password` string 必填
  - `rememberMe` boolean 选填
- 返回: `R<TokenResponseDTO>`（包含 `accessToken`、`refreshToken` 及过期信息）

### 4.2 刷新令牌

- Method: `POST`
- URL: `/api/v1/auth/refresh`
- Auth: 否
- Header:
  - `Refresh-Token: Bearer <refresh_token>`
- 返回: `R<TokenResponseDTO>`

### 4.3 登出

- Method: `POST`
- URL: `/api/v1/auth/logout`
- Auth: 是（需 `Authorization`）
- Header（可选）:
  - `Refresh-Token: Bearer <refresh_token>`
- 返回: `R<Void>`

## 5. YOLO 节点管理模块

### 5.1 分页查询节点

- Method: `GET`
- URL: `/api/v1/yolo-nodes/page`
- Auth: 是
- Query:
  - `current` int 默认 `1`
  - `size` int 默认 `10`
  - `nodeName` string 选填
  - `status` int 选填
- 返回: `R<IPage<YoloNodeVO>>`

### 5.2 查询节点详情

- Method: `GET`
- URL: `/api/v1/yolo-nodes/{id}`
- Auth: 是
- Path:
  - `id` long
- 返回: `R<YoloNodeDetailVO>`（含权重列表与参数信息）

### 5.3 新增节点

- Method: `POST`
- URL: `/api/v1/yolo-nodes`
- Auth: 是
- Content-Type: `application/json`
- Body (`YoloNodeSaveDTO`):
  - `nodeName` 必填
  - `description` 选填
  - `host` 必填
  - `port` 必填
  - `httpProtocol` 选填（默认 `http`）
  - `apiVersion` 选填（默认 `v1`）
- 返回: `R<Void>`

### 5.4 更新节点

- Method: `PUT`
- URL: `/api/v1/yolo-nodes/{id}`
- Auth: 是
- Content-Type: `application/json`
- Path:
  - `id` long
- Body: 同 `YoloNodeSaveDTO`
- 返回: `R<Void>`

### 5.5 删除节点

- Method: `DELETE`
- URL: `/api/v1/yolo-nodes/{id}`
- Auth: 是
- Path:
  - `id` long
- 返回: `R<Void>`（会级联删除关联数据）

### 5.6 应用参数模板

- Method: `PUT`
- URL: `/api/v1/yolo-nodes/{id}/params/template/{templateName}/apply`
- Auth: 是
- Path:
  - `id` long
  - `templateName` string
- 返回: `R<Void>`

### 5.7 手动同步节点状态

- Method: `POST`
- URL: `/api/v1/yolo-nodes/{id}/sync`
- Auth: 是
- Path:
  - `id` long
- 返回: `R<Void>`

### 5.8 上传节点权重

- Method: `POST`
- URL: `/api/v1/yolo-nodes/{id}/weights`
- Auth: 是
- Content-Type: `multipart/form-data`
- Path:
  - `id` long
- Form:
  - `file` 文件
- 返回: `R<Void>`

### 5.9 切换活跃权重

- Method: `PUT`
- URL: `/api/v1/yolo-nodes/{id}/weights/{filename}/active`
- Auth: 是
- Path:
  - `id` long
  - `filename` string
- 返回: `R<Void>`

### 5.10 删除节点权重

- Method: `DELETE`
- URL: `/api/v1/yolo-nodes/{id}/weights/{filename}`
- Auth: 是
- Path:
  - `id` long
  - `filename` string
- 返回: `R<Void>`

## 6. 推理模块

### 6.1 发起图片推理

- Method: `POST`
- URL: `/api/v1/inference/nodes/{nodeId}/image`
- Auth: 是
- Content-Type: `multipart/form-data`
- Path:
  - `nodeId` long
- Form:
  - `file` 图片文件
- 返回: `R<YoloDetectionRecordEntity>`

### 6.2 发起视频推理

- Method: `POST`
- URL: `/api/v1/inference/nodes/{nodeId}/video`
- Auth: 是
- Content-Type: `multipart/form-data`
- Path:
  - `nodeId` long
- Form:
  - `file` 视频文件
- 返回: `R<YoloDetectionRecordEntity>`

### 6.3 分页查询推理记录

- Method: `GET`
- URL: `/api/v1/inference/records/page`
- Auth: 是
- Query:
  - `current` int 默认 `1`
  - `size` int 默认 `10`
  - `nodeId` long 选填
  - `taskType` int 选填（通常 1=图片，2=视频）
  - `originalFilename` string 选填
- 返回: `R<IPage<YoloDetectionRecordEntity>>`

### 6.4 查询单条推理记录

- Method: `GET`
- URL: `/api/v1/inference/records/{id}`
- Auth: 是
- Path:
  - `id` long
- 返回: `R<YoloDetectionRecordEntity>`

### 6.5 删除推理记录

- Method: `DELETE`
- URL: `/api/v1/inference/records/{id}`
- Auth: 是
- Path:
  - `id` long
- 返回: `R<Void>`

## 7. 用户个人中心模块

### 7.1 获取当前登录用户资料

- Method: `GET`
- URL: `/api/v1/users/profile`
- Auth: 是
- 返回: `R<UserProfileVO>`

`UserProfileVO` 常用字段：

- `id` long 用户ID
- `username` string 用户名
- `nickname` string 昵称
- `avatarUrl` string 头像地址
- `email` string 邮箱
- `phoneNumber` string 手机号
- `gender` int 性别（0=未知, 1=男, 2=女）
- `introduction` string 个人简介
- `lastLoginTime` string 最近登录时间
- `roles` string[] 角色编码列表
- `permissions` string[] 权限编码列表

安全说明：

- 不返回 `password` 等敏感字段。

### 7.2 修改当前登录用户资料

- Method: `PUT`
- URL: `/api/v1/users/profile`
- Auth: 是
- Content-Type: `application/json`
- Body (`UserProfileUpdateDTO`):
  - `nickname` string 必填，最大 64 字符
  - `email` string 选填，需符合邮箱格式
  - `phoneNumber` string 选填，需符合中国大陆手机号格式
  - `avatarUrl` string 选填，头像 URL
  - `gender` int 选填，`0=未知, 1=男, 2=女`
  - `introduction` string 选填，最大 500 字符
- 返回: `R<UserProfileVO>`（`msg` 通常为“个人资料修改成功”，`data` 为更新后的最新资料）

业务约束补充：

- 修改邮箱时会做唯一性校验。
- 修改手机号时会做唯一性校验。

## 8. 系统文件管理模块

### 8.1 通用文件上传

- Method: `POST`
- URL: `/api/v1/files/upload`
- Auth: 是
- Content-Type: `multipart/form-data`
- Form:
  - `file` 文件（必填）
  - `dir` string 选填，默认 `common`
- 返回: `R<String>`（`data` 为可访问文件 URL）

### 8.2 上传用户头像

- Method: `POST`
- URL: `/api/v1/files/upload/avatar`
- Auth: 是
- Content-Type: `multipart/form-data`
- Form:
  - `file` 图片文件（必填）
- 返回: `R<String>`（`data` 为头像 URL）

业务约束补充：

- 仅支持 `JPG/PNG`（`image/jpeg`, `image/jpg`, `image/png`）。
- 头像大小限制为 `2MB`。

## 9. 错误响应说明

- 鉴权失败通常返回 `401/403`（由安全框架处理）。
- 参数缺失、业务异常由全局异常处理返回 `R` 结构。
- `auth/refresh` 在缺失或非法 `Refresh-Token` 时会返回业务错误码与错误消息。

## 10. 备注

建议以运行时 OpenAPI 为准进行最终联调：

- OpenAPI JSON: `/api/v1/v3/api-docs`
- Swagger UI: `/api/v1/swagger-ui/index.html`
