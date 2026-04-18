# UAV System

UAV System 是一个前后端分离 + AI 推理服务的多模块项目，当前代码按领域统一收敛在 projects 目录下，便于独立开发与统一部署。

## 文档适用范围

- 本 README 与根目录下两份中文说明文档，面向当前主工程：`projects/backend-core`、`projects/uav-frontend`、`projects/yolo-service`。
- `backup/` 与 `projects/yolo-service/libs/` 下的第三方或历史文档仅作参考，不作为当前交付基线。

## 项目结构

```text
uav-system/
├── projects/
│   ├── backend-core/     # Spring Boot 业务后端
│   ├── uav-frontend/     # Vue 3 + TypeScript 前端管理台
│   └── yolo-service/     # FastAPI + YOLO 推理服务
├── backup/               # 历史备份与第三方资料
├── 项目安装部署说明.md      # 安装、配置、启动、部署指引
└── 组件使用情况说明.md      # 模块职责、调用链路、配置说明
```

## 模块说明

- projects/backend-core
  - 业务 API、鉴权、RBAC、推理记录管理
  - 默认端口 8111（context-path: /api/v1）
- projects/uav-frontend
  - 管理台前端，调用 backend-core 提供的业务接口
  - 默认端口 5173
- projects/yolo-service
  - 模型推理、权重与参数管理
  - 默认端口 8000

## 本地开发启动顺序

1. 启动基础设施：MySQL、Redis、MinIO
2. 启动推理服务（详细命令见 [项目安装部署说明.md - 9.1 启动 yolo-service](项目安装部署说明.md#91-启动-yolo-service)）
3. 启动后端服务（详细命令见 [项目安装部署说明.md - 9.2 启动 backend-core](项目安装部署说明.md#92-启动-backend-core)）
4. 启动前端服务（详细命令见 [项目安装部署说明.md - 9.3 启动 uav-frontend](项目安装部署说明.md#93-启动-uav-frontend)）

说明：

- 根 README 仅保留启动顺序与联调入口。
- 具体命令（含 Windows/Linux 差异）以 [项目安装部署说明.md](项目安装部署说明.md) 为唯一维护来源。

## 快速联调检查

启动完成后，建议按顺序检查：

1. 前端首页：`http://localhost:5173`
2. 后端 Swagger：`http://localhost:8111/api/v1/swagger-ui/index.html`
3. 算法服务健康检查：`http://localhost:8000/api/v1/health`

## 核心文档

- [项目安装部署说明.md](项目安装部署说明.md)
- [组件使用情况说明.md](组件使用情况说明.md)
- [projects/backend-core/API.md](projects/backend-core/API.md)
- [projects/backend-core/PERMISSION_MATRIX.md](projects/backend-core/PERMISSION_MATRIX.md)
- [projects/uav-frontend/README.md](projects/uav-frontend/README.md)
- [projects/yolo-service/README.md](projects/yolo-service/README.md)
- [projects/yolo-service/API.md](projects/yolo-service/API.md)

## 备注

- backup 目录主要用于历史版本与第三方代码存档，不作为当前业务代码的主入口。
- 以 projects 目录下三个子项目为当前迭代与部署的基准。
