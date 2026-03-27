这份 `README.md` 是专为你的 **UAV 车辆检测微服务** 定制的。它结合了你目前的项目结构、`uv` 包管理工具以及魔改后的本地 `ultralytics` 库。

你可以直接将其复制到项目根目录下。

-----

# UAV Vehicle Detection Microservice

基于 **FastAPI** 和 **YOLO** 的无人机视角车辆小目标检测微服务。本项目作为无人机管理系统的算法支撑模块，专为 Java Spring Boot 后端提供高性能的图像与视频识别接口。

-----

## 核心特性

* **高性能推理**：集成魔改后的本地 `ultralytics` 库，支持 YOLOv8/v11 架构。
* **工程化设计**：采用 `uv` 进行依赖管理，支持 `Editable` 模式挂载本地源码，方便实时调试算法。
* **模块化架构**：路由（Routers）、核心逻辑（Core）、配置（Config）完全解耦。
* **全格式支持**：支持单张图片识别及 `.mp4` 视频流异步/同步检测。
* **配置隔离**：通过 `.env` 实现环境敏感参数与代码逻辑的彻底分离。

-----

## 技术栈

* **Language**: Python 3.10+
* **Framework**: FastAPI, Uvicorn
* **AI Engine**: PyTorch, Ultralytics (Local Editable Version)
* **Package Manager**: [uv](https://github.com/astral-sh/uv)
* **Config Management**: Pydantic Settings

-----

## 项目结构

```text
yolo-service/
```

-----

## 快速开始

### 1\. 环境准备

确保已安装 `uv`。如果未安装，可通过以下命令安装：

```powershell
powershell -c "irm https://astral-sh.uv.io/install.ps1 | iex"
```

### 2\. 初始化与安装

在项目根目录下执行，`uv` 会自动解析 `libs/ultralytics` 及其依赖：

```powershell
# 创建虚拟环境
uv venv

# 挂载本地魔改库并安装所有依赖 (FastAPI, Torch, OpenCV 等)
uv add --editable ./libs/ultralytics
```

### 3\. 配置环境

创建 `.env` 文件并根据实际情况修改路径：

```ini
PROJECT_NAME="UAV 车辆检测微服务"
APP_PORT=8000
MODEL_WEIGHTS_PATH="weights/yolo26n.pt"
```

### 4\. 启动服务

```powershell
uv run python app/main.py
```

-----

## 接口文档 (API Endpoints)

服务启动后，访问 `http://localhost:8000/docs` 查看完整 Swagger 交互文档。

### 图像检测

- **POST** `/api/v1/detect/image`
- **Payload**: `multipart/form-data` (file: Image)
- **Response**: 返回识别到的车辆坐标（xmin, ymin, xmax, ymax）、置信度及类别。

### 视频检测

- **POST** `/api/v1/video/upload`
- **Payload**: `multipart/form-data` (file: mp4/avi)
- **Response**: 返回处理后的视频保存路径，支持后台异步清理原文件。

-----

## 开发指南

* **修改算法**：直接修改 `libs/ultralytics` 中的源码，主服务无需重启（若开启了 `reload` 模式）即可实时生效。
* **添加新路由**：在 `app/routers/` 目录下新建模块，并在 `main.py` 中通过 `app.include_router()` 挂载。
* **部署建议**：建议在 Linux 环境下配合 NVIDIA 驱动使用 GPU 加速。