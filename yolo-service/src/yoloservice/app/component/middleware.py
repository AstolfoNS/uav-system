
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from yoloservice.config.settings import settings
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from starlette.exceptions import HTTPException as StarletteHTTPException

from yoloservice.common.schema.response import R

def setup_middlewares(app: FastAPI):
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.ALLOWED_ORIGINS,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    # 重写 404 / 500 等标准 HTTP 异常
    @app.exception_handler(StarletteHTTPException)
    async def http_exception_handler(request: Request, exc: StarletteHTTPException):
        return JSONResponse(
            status_code=exc.status_code,
            content=R.fail(code=exc.status_code, msg=exc.detail).model_dump()
        )

    # 重写 422 参数校验异常（比如前端传错参数）
    @app.exception_handler(RequestValidationError)
    async def validation_exception_handler(request: Request, exc: RequestValidationError):
        # 拼接校验错误信息
        errors = exc.errors()
        msg = f"参数校验失败: {errors[0]['msg']} 在字段 {'.'.join(str(x) for x in errors[0]['loc'])}"
        return JSONResponse(
            status_code=422,
            content=R.fail(code=422, msg=msg).model_dump()
        )