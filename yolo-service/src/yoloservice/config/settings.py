from pathlib import Path
from typing import List

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict

from yoloservice import BASE_ROOT, SRC_ROOT, PROJECT_ROOT
from yoloservice.config.log.logging_settings import LoggingSettings


class Settings(BaseSettings):
    # 这里的变量名必须和 .env 中完全一致（不区分大小写）
    PROJECT_NAME: str = "UAV Detection Service"
    PROJECT_DESC: str = "API for yolo UAV Detection Service."
    PROJECT_VERSION: str = "1.0.0"

    API_HOST: str = "0.0.0.0"
    API_PORT: int = 8000
    API_PREFIX: str = "/api/v1"

    DEBUG: bool = True

    BASE_DIR: Path = BASE_ROOT
    SRC_DIR: Path = SRC_ROOT
    PROJECT_DIR: Path = PROJECT_ROOT

    WORKERS: int = 1
    ALLOWED_ORIGINS: List[str] = ["*"]

    MODEL_WEIGHTS_PATH: str = "weights/yolo26n.pt"
    UPLOAD_DIR: str = "data/temp_uploads"
    OUTPUT_DIR: str = "runs/detect"
    STATIC_URL: str = "/static"

    # MinIO 配置
    MINIO_ENDPOINT: str = "192.168.14.129:9000"
    MINIO_ACCESS_KEY: str = "admin"
    MINIO_SECRET_KEY: str = "1234567890"
    MINIO_SECURE: bool = False
    MINIO_BUCKET_NAME: str = "uav-system"

    LOG: LoggingSettings = Field(default_factory=LoggingSettings)

    # 读取 .env 文件
    model_config = SettingsConfigDict(
        env_file=BASE_DIR / ".env",
        env_file_encoding='utf-8',
        extra='ignore' # 忽略环境变量中多余的参数
    )

    @property
    def absolute_weights_path(self) -> str:
        """获取权重的绝对路径"""
        path = BASE_ROOT / self.MODEL_WEIGHTS_PATH
        return str(path) if path.exists() else self.MODEL_WEIGHTS_PATH

# 实例化全局对象
settings = Settings()

__all__ = ["settings"]