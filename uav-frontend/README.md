# UAV Frontend

UAV inference and recognition management frontend built with Vue 3 + TypeScript + Vuetify.

This frontend only calls backend-core APIs.

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
- YOLO node management
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

Install dependencies:

```bash
npm install
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

## Main Pages

- `/login`: Sign-in page
- `/dashboard`: System overview
- `/nodes`: YOLO node management
- `/inference`: Image/video inference tasks
- `/records`: Inference record management
