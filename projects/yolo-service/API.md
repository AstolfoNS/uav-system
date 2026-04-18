# UAV Detection Service API 文档

## 文档导航

- 返回总览：[../../README.md](../../README.md)
- 安装部署：[../../项目安装部署说明.md](../../项目安装部署说明.md)
- 模块说明：[../../组件使用情况说明.md](../../组件使用情况说明.md)

本文档对应 `yolo-service`（FastAPI）当前实现。

## 1. 基础信息

- 服务默认地址: `http://<host>:8000`
- 业务接口前缀: `/api/v1`
- 业务接口完整前缀: `http://<host>:8000/api/v1`
- OpenAPI: `/openapi.json`
- Swagger UI: `/docs`
- 路由浏览页（自定义首页）: `/`
- 本地联调示例:
  - `http://127.0.0.1:8000`
  - `http://127.0.0.1:8000/api/v1`
  - `http://127.0.0.1:8000/docs`

## 1.1 鉴权说明

- 当前 `yolo-service` 作为内部算法服务，默认未启用鉴权。
- 建议仅在内网访问，或由 `backend-core` 统一代理转发，避免公网直连。
- 生产环境建议：
  - 通过网关统一做鉴权、限流与审计。
  - 仅开放必要端口并限制来源 IP。
  - 禁止服务端口直接暴露在公网。

## 2. 响应结构说明

当前服务存在两套成功响应风格（与代码一致）：

### 2.1 Inference 接口（image/video）

返回统一 `R` 结构，字段为 `code/msg/data`。

```json
{
  "code": 200,
  "msg": "图像识别成功",
  "data": {}
}
```

### 2.2 Weights/Params 接口

成功响应中使用 `message` 字段（不是 `msg`），例如：

```json
{
  "code": 200,
  "message": "获取成功",
  "data": {}
}
```

### 2.3 错误响应

- `image/video` 路由内部返回 `R.fail(...)`，通常是 `code/msg/data`。
- `weights/params` 多通过 `HTTPException` 抛错，再由中间件包装成 `code/msg/data`，并带对应 HTTP 状态码。
- 参数校验失败由中间件统一返回 `422`，结构为 `code/msg/data`。

## 3. 健康检查

### 3.1 探活接口

- Method: `GET`
- URL: `/api/v1/health`
- Auth: 否
- 返回示例:

```json
{
  "status": "ok"
}
```

## 4. 推理接口（Inference）

### 4.1 图像预测

- Method: `POST`
- URL: `/api/v1/image/predict`
- Auth: 否
- Content-Type: `multipart/form-data`
- Form:
  - `file` 图片文件（`content-type` 需以 `image/` 开头）

成功返回示例：

```json
{
  "code": 200,
  "msg": "图像识别成功",
  "data": {
    "count": 2,
    "detections": [
      {
        "class_id": 0,
        "class_name": "uav",
        "confidence": 0.8912,
        "bbox": [105.2, 50.1, 204.8, 120.3]
      }
    ],
    "image_url": "http://127.0.0.1:9000/uav-system/images/image_xxx.jpg"
  }
}
```

失败示例（类型错误）：

```json
{
  "code": 400,
  "msg": "仅支持上传图片文件",
  "data": null
}
```

### 4.2 视频预测

- Method: `POST`
- URL: `/api/v1/video/predict`
- Auth: 否
- Content-Type: `multipart/form-data`
- Form:
  - `file` 视频文件（支持扩展名: `.mp4/.avi/.mov/.mkv`）

成功返回示例：

```json
{
  "code": 200,
  "msg": "视频识别成功",
  "data": {
    "video_url": "http://127.0.0.1:9000/uav-system/videos/video_xxx_h264.mp4",
    "message": "视频推理及渲染已完成"
  }
}
```

失败示例（格式不支持）：

```json
{
  "code": 400,
  "msg": "仅支持 mp4, avi, mov, mkv 格式的视频文件。",
  "data": null
}
```

## 5. 模型权重管理（Weights）

### 5.1 获取权重列表

- Method: `GET`
- URL: `/api/v1/weights/`
- Auth: 否
- 返回示例：

```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "active": "yolo26n.pt",
    "available": ["yolo26n.pt", "sky-scan-det_640_fp16.onnx"]
  }
}
```

### 5.2 上传权重

- Method: `POST`
- URL: `/api/v1/weights/`
- Auth: 否
- Content-Type: `multipart/form-data`
- Form:
  - `file` 权重文件（支持 `.pt/.onnx`）
- 返回示例：

```json
{
  "code": 200,
  "message": "权重文件上传成功",
  "data": {
    "filename": "yolov8x_best.pt"
  }
}
```

### 5.3 切换活跃权重

- Method: `PUT`
- URL: `/api/v1/weights/{filename}/active`
- Auth: 否
- Path:
  - `filename` 文件名（建议传包含后缀的完整文件名）
- 返回示例：

```json
{
  "code": 200,
  "message": "成功切换当前活跃模型至 yolov8x_best.pt"
}
```

### 5.4 删除权重

- Method: `DELETE`
- URL: `/api/v1/weights/{filename}`
- Auth: 否
- Path:
  - `filename` 文件名（建议传包含后缀的完整文件名）
- 返回示例：

```json
{
  "code": 200,
  "message": "成功删除 yolov8x_best.pt"
}
```

### 5.5 权重运行依赖说明

- `.pt`: 使用 PyTorch/Ultralytics，默认可用。
- `.onnx`: 需要 `onnx` 与 `onnxruntime`。

当依赖缺失时，接口会返回明确错误提示。

## 6. 推理参数管理（Params）

### 6.1 查询当前参数

- Method: `GET`
- URL: `/api/v1/params/`
- Auth: 否
- 返回示例：

```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "conf": 0.25,
    "iou": 0.7
  }
}
```

### 6.2 批量更新参数

- Method: `POST`
- URL: `/api/v1/params/`
- Auth: 否
- Content-Type: `application/json`
- Body 示例：

```json
{
  "params": {
    "conf": 0.5,
    "iou": 0.65,
    "imgsz": 640
  }
}
```

- 返回示例：

```json
{
  "code": 200,
  "message": "批量参数更新成功",
  "data": {
    "conf": 0.5,
    "iou": 0.65,
    "imgsz": 640
  }
}
```

### 6.3 设置单个参数

- Method: `PUT`
- URL: `/api/v1/params/{key}`
- Auth: 否
- Content-Type: `application/json`
- Path:
  - `key` 参数名
- Body 示例：

```json
{
  "value": 0.82
}
```

- 返回示例：

```json
{
  "code": 200,
  "message": "参数 conf 更新成功",
  "data": {
    "conf": 0.82
  }
}
```

### 6.4 删除单个参数

- Method: `DELETE`
- URL: `/api/v1/params/{key}`
- Auth: 否
- Path:
  - `key` 参数名
- 返回示例：

```json
{
  "code": 200,
  "message": "参数 iou 删除成功"
}
```

## 7. 联调建议

- 优先以 `/docs` 中的实时 OpenAPI 定义为准。
- 如前端需要统一 `msg/message` 字段，建议服务端后续做响应格式收敛。

## 8. 最小联调流程（curl 示例）

1. 健康检查

```bash
curl "http://localhost:8000/api/v1/health"
```

2. 查询权重列表

```bash
curl "http://localhost:8000/api/v1/weights/"
```

3. 查询当前参数

```bash
curl "http://localhost:8000/api/v1/params/"
```

4. 上传图片推理

```bash
curl -X POST "http://localhost:8000/api/v1/image/predict" \
  -F "file=@./demo.jpg"
```
