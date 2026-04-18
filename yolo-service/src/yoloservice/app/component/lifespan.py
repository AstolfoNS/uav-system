import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from yoloservice.config.settings import settings
from yoloservice.core.detector import yolo_manager


logger = logging.getLogger("yolo-service")


def _log_ultralytics_runtime_info() -> None:
    """记录推理运行时实际使用的 ultralytics 路径与关键模块可用性。"""
    try:
        import ultralytics
        import ultralytics.nn.modules.conv as conv_module

        ultra_file = getattr(ultralytics, "__file__", "<unknown>")
        conv_file = getattr(conv_module, "__file__", "<unknown>")
        has_pconv = hasattr(conv_module, "PConv")

        logger.info(f"Ultralytics runtime path: {ultra_file}")
        logger.info(f"Ultralytics conv module path: {conv_file}")
        logger.info(f"Ultralytics PConv available: {has_pconv}")

        if not has_pconv:
            logger.warning(
                "当前运行时未检测到 PConv，若加载包含 PConv 结构的 .pt 权重将失败。"
            )
    except Exception as e:
        logger.warning(f"读取 Ultralytics 运行时信息失败: {e}")


@asynccontextmanager
async def lifespan(_app: FastAPI):
    _log_ultralytics_runtime_info()

    # 启动时加载模型
    yolo_manager.load_model(settings.absolute_weights_path)
    yield

    logger.info("Service is closing...")

