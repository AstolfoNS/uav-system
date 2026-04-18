import logging
import os

from fastapi import FastAPI
from yoloservice.app.component.logging import setup_logging
from yoloservice.app.component.lifespan import lifespan
from yoloservice.app.component.middleware import setup_middlewares
from yoloservice.app.component.route import setup_routes
from yoloservice.config.settings import settings


logger = logging.getLogger("yolo-service")


def print_fastapi():
    print(r"""
     ________               _        _       _______  _____  
    |_   __  |             / |_     / \     |_   __ \|_   _| 
      | |_ \_|,--.   .--. `| |-'   / _ \      | |__) | | |   
      |  _|  `'_\ : ( (`\] | |    / ___ \     |  ___/  | |   
     _| |_   // | |, `'.'. | |, _/ /   \ \_  _| |_    _| |_  
    |_____|  \'-;__/[\__) )\__/|____| |____||_____|  |_____| 
                                                             
    """)


def create_app() -> FastAPI:
    if not os.environ.get("APP_BANNER_PRINTED"):
        print_fastapi()
        os.environ["APP_BANNER_PRINTED"] = "true"

    # 日志系统初始化
    setup_logging()
    logger.info("Logging system initialized")

    # 创建 FastAPI 实例
    logger.info(
        "Creating FastAPI application "
        f"(env={'DEBUG' if settings.DEBUG else 'PROD'})"
    )
    app = FastAPI(
        title=settings.PROJECT_NAME,
        description=settings.PROJECT_DESC,
        version=settings.PROJECT_VERSION,
        lifespan=lifespan,
    )

    # 中间件
    setup_middlewares(app)
    logger.info("Middlewares registered")

    # 统一注册路由
    setup_routes(app)
    logger.info("Routes registered")

    # 应用就绪
    logger.info(
        "Application created successfully "
        f"(name={settings.PROJECT_NAME}, version={settings.PROJECT_VERSION})"
    )

    return app
