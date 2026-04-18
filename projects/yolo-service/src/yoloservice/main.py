from yoloservice.app.create_app import create_app
from yoloservice.config.settings import settings

app = create_app()

if __name__ == "__main__":
    import uvicorn

    run_config = {
        "app": "main:app",
        "host": settings.API_HOST,
        "port": settings.API_PORT,
        "log_level": "info",
    }

    if settings.DEBUG:
        # 开发模式：开启热重载，单进程
        run_config["reload"] = True
    else:
        # 生产模式：关闭热重载，开启多进程
        run_config["workers"] = settings.WORKERS
        run_config["reload"] = False

    uvicorn.run(**run_config)
