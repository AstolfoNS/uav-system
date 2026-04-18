import cv2
import numpy as np
from fastapi import APIRouter, UploadFile, File, Request
from starlette.concurrency import run_in_threadpool

from yoloservice.web.router import register_router
from yoloservice.service.inference_service import inference_service
from yoloservice.common.schema.response import R


@register_router
def create_image_router():
    router = APIRouter(prefix="/image", tags=["Image"])

    @router.post("/predict")
    async def predict_image(request: Request, file: UploadFile = File(...)):
        if not file.content_type or not file.content_type.startswith("image/"):
            return R.fail(code=400, msg="仅支持上传图片文件")

        try:
            image_bytes = await file.read()
            request_host_url = str(request.base_url)

            # 将解码和推理任务卸载到线程池
            result_dict = await run_in_threadpool(
                inference_service.process_image, 
                image_bytes, 
                request_host_url
            )

            return R.ok(data=result_dict, msg="图像识别成功")

        except ValueError as e:
            return R.fail(code=400, msg=str(e))
        except Exception as e:
            return R.fail(code=500, msg=f"模型推理异常: {str(e)}")

    return router
