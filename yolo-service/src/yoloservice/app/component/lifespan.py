import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from yoloservice.config.settings import settings
from yoloservice.core.detector import yolo_manager


logger = logging.getLogger("yolo-service")


@asynccontextmanager
async def lifespan(_app: FastAPI):
    # 启动时加载模型
    yolo_manager.load_model(settings.absolute_weights_path)
    yield

    logger.info("Service is closing...")

