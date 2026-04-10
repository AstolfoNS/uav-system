# UAV Detection Service — API 接口文档

**基础路径 (Base URL):** `http://<服务器IP>:<端口>/api/v1`
**当前项目版本:** `1.0.0`

本文档包含了当前项目中所有的 RESTful API 接口，用于图像/视频推理预测、模型权重管理与预测参数的动态配置。

---

## 1. 业务功能: 核心目标检测 (Inference)

### 1.1 图像预测
**接口：** `POST /image/predict`
**描述：** 上传单张图片进行目标检测（UAV）。模型会识别图片中的对象，并将渲染好的带有边界框和置信度的生成图上传至 MinIO，并返回远程 URL 和解析结果。
**Content-Type:** `multipart/form-data`

| 字段名称 | 类型 | 必填 | 描述 |
| :--- | :--- | :--- | :--- |
| `file` | file | 是 | 需要预测识别的图像文件（例如 .jpg, .png）。 |

**响应成功示例:**
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
    "image_url": "http://192.168.14.129:9000/uav-system/images/image_xxx.jpg"
  }
}
```

---

### 1.2 视频预测
**接口：** `POST /video/predict`
**描述：** 上传视频片段进行全过程的检测与推演。系统内部会使用 moviepy 将输出视频转码为 H.264 格式后自动上传至 MinIO，并返回远程 URL。
**Content-Type:** `multipart/form-data`

| 字段名称 | 类型 | 必填 | 描述 |
| :--- | :--- | :--- | :--- |
| `file` | file | 是 | 需要预测的高清视频文件（支持 mp4, avi, mov, mkv 等）。 |

**响应成功示例:**
```json
{
  "code": 200,
  "msg": "视频识别成功",
  "data": {
    "video_url": "http://192.168.14.129:9000/uav-system/videos/video_xxx_h264.mp4",
    "message": "视频推理及渲染已完成"
  }
}
```

---

## 2. 核心功能: 模型权重管理 (Weights CRUD)

本组接口用于直接管理运行实例后端的 YOLO 模型 `.pt` 权重文件，能够在无须重启进程的情况下上传并重载服务模型。

### 2.1 获取已有模型列表及当前状态
**接口：** `GET /weights`
**描述：** 返回目前 `weights/` 目录下存放的所有受支持的权重文件，并显示服务目前正在使用的活跃版本。

**响应成功示例:**
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "active": "yolo26n.pt",
    "available": ["yolo26n.pt", "yolov8m_custom.pt"]
  }
}
```

### 2.2 上传新模型权重
**接口：** `POST /weights`
**描述：** 上传 `.pt` 模型文件以入驻后端本地仓库，备日后切换调用。
**Content-Type:** `multipart/form-data`

| 字段名称 | 类型 | 必填 | 描述 |
| :--- | :--- | :--- | :--- |
| `file` | file | 是 | 限定 `.pt` 扩展名的 PyTorch / YOLO 模型网络权重文件。 |

**响应成功示例:**
```json
{
  "code": 200,
  "message": "权重文件上传成功",
  "data": {
    "filename": "yolov8x_best.pt"
  }
}
```

### 2.3 切换/设定活跃模型 (Active)
**接口：** `PUT /weights/{filename}/active`
**描述：** 让系统在内存里立即释放旧模型，并指定 `{filename}` 为接下来所有的检测业务执行主计算模型。

**路径参数:**
- `filename`: 所指定的目标模型库文件名 (如 `yolov8x_best.pt`)。

**响应成功示例:**
```json
{
  "code": 200,
  "message": "成功切换当前活跃模型至 yolov8x_best.pt"
}
```

### 2.4 删除限制模型权重
**接口：** `DELETE /weights/{filename}`
**描述：** 从后端删除无用的或陈旧的系统模型。系统会进行拦截检查：防止请求企图删除当前正被服务占用的活跃模型。

**路径参数:**
- `filename`: 即将删除的目标模型库文件名。

**响应成功示例:**
```json
{
  "code": 200,
  "message": "成功删除 yolov8x_best.pt"
}
```

---

## 3. 实时辅助: 推理参数设置 (Params CRUD)

全局动态作用于底层原生 `model.predict()` 调用方法，提供极高的调试/业务灵活性。以下所有生效更改皆从设置完成起的下一次识别立刻开始生效。

### 3.1 查询当前各项推理参数
**接口：** `GET /params`
**描述：** 拉取当前生效的所有定制化预测参数。

**响应成功示例:**
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

### 3.2 批量全覆写/设置模型参数
**接口：** `POST /params`
**描述：** 批量合并及更新指定的预测参数群（如更改 `conf`、增大 `imgsz` 等）。
**Content-Type:** `application/json`

**请求 Body 结构示例:**
```json
{
  "params": {
    "conf": 0.5,
    "iou": 0.65,
    "imgsz": 640
  }
}
```

**响应成功示例:**
```json
{
  "code": 200,
  "message": "批量参数更新成功",
  "data": {
    ...
  }
}
```

### 3.3 修改具体独立参数
**接口：** `PUT /params/{key}`
**描述：** 提供单一浮点数的精确更新与覆盖配置。
**Content-Type:** `application/json`

**路径参数:**
- `key`: 需要设置更新的键名（如 `conf` 或 `iou` 或 `vid_stride` 等 ultralytics 支持的所有参数名称）。

**请求 Body 结构示例:**
```json
{
  "value": 0.82
}
```

**响应成功示例:**
```json
{
  "code": 200,
  "message": "参数 conf 更新成功",
  "data": {
    "conf": 0.82
  }
}
```

### 3.4 删除/归零指定独立参数
**接口：** `DELETE /params/{key}`
**描述：** 清除配置系统内的指定独立参数改动。这意味着该参数在下次侦测时将被重新重置为 YOLO Ultralytics 本原的框架引擎默认配置。

**路径参数:**
- `key`: 需要删除丢弃的键名。

**响应成功示例:**
```json
{
  "code": 200,
  "message": "参数 iou 删除成功"
}
```
