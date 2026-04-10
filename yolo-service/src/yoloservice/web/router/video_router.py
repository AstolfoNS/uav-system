import os
import shutil
from fastapi import APIRouter, UploadFile, File, Request
from starlette.concurrency import run_in_threadpool

from yoloservice.web.router import register_router
from yoloservice.service.inference_service import inference_service
from yoloservice.config.settings import settings
from yoloservice.common.schema.response import R


@register_router
def create_video_router():
    router = APIRouter(prefix="/video", tags=["Video"])

    @router.post("/predict")
    async def predict_video(request: Request, file: UploadFile = File(...)):
        if not file.filename.lower().endswith(('.mp4', '.avi', '.mov', '.mkv')):
            return R.fail(code=400, msg="仅支持 mp4, avi, mov, mkv 格式的视频文件。")

        from yoloservice.common.utils.file_utils import generate_uuid_name, get_ext
        ext = get_ext(file.filename)
        
        os.makedirs(settings.UPLOAD_DIR, exist_ok=True)
        temp_file_path = os.path.join(settings.UPLOAD_DIR, f"upload_{generate_uuid_name()}{ext}")

        try:
            with open(temp_file_path, "wb") as buffer:
                shutil.copyfileobj(file.file, buffer)

            request_host_url = str(request.base_url)

            result_data = await run_in_threadpool(
                inference_service.process_video,
                temp_file_path,
                request_host_url
            )

            return R.ok(data=result_data, msg="视频识别成功")

        except ValueError as e:
            return R.fail(code=400, msg=str(e))
        except Exception as e:
            return R.fail(code=500, msg=f"服务器内部推理错误: {str(e)}")

        finally:
            if os.path.exists(temp_file_path):
                os.remove(temp_file_path)

    return router
