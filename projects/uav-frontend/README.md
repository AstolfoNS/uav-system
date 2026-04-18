# UAV Frontend

## 文档导航

- 返回总览：[../../README.md](../../README.md)
- 安装部署：[../../项目安装部署说明.md](../../项目安装部署说明.md)
- 模块说明：[../../组件使用情况说明.md](../../组件使用情况说明.md)

UAV inference and recognition management frontend built with Vue 3 + TypeScript + Vuetify.

This frontend only calls backend-core APIs.

## Runtime Requirements

- Node.js 20.x LTS (recommended)
- npm 10+ (or pnpm 9+)

## Tech Stack

- Vue 3
- TypeScript
- Vue Router
- Vuetify
- Vite

## Features

- Authentication with token persistence
- Auto refresh token when access token expires
- Dashboard with node and record overview
- Model node management
  - Create, edit, delete nodes
  - Sync node state
  - Apply parameter template
  - Upload/switch/delete node weights
- Inference task center
  - Upload image for inference
  - Upload video for inference
  - Display response payload and media preview when URL is available
- Record management
  - Filter and paginate records
  - View record details
  - Delete records

## API Configuration

Set backend-core base URL using environment variable:

- `VITE_API_BASE_URL`

Default value if not set:

- `http://localhost:8111/api/v1`

## Local Development

Set environment variable first (create `.env` if missing):

```ini
VITE_API_BASE_URL=http://localhost:8111/api/v1
```

Install dependencies:

```bash
npm install
```

If you prefer pnpm:

```bash
pnpm install
```

Start dev server:

```bash
npm run dev
```

Build production bundle:

```bash
npm run build
```

Preview production bundle:

```bash
npm run preview
```

i18n consistency check:

```bash
npm run i18n:check
```

## Main Pages

- `/login`: Sign-in page
- `/dashboard`: System overview
- `/nodes`: Model node management
- `/inference`: Image/video inference tasks
- `/records`: Inference record management
- `/profile`: User profile settings
- `/rbac`: RBAC management (admin role only)
