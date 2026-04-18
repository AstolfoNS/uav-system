from fastapi import APIRouter, File, UploadFile, HTTPException
from yoloservice.web.router import register_router
from yoloservice.service.weight_service import weight_service

def build_weight_router() -> APIRouter:
    router = APIRouter(prefix="/weights", tags=["weights"])

    @router.get("/")
    async def list_weights():
        """
        获取系统内所有已上传的权重文件列表及当前激活的模型版本。
        """
        try:
            data = weight_service.list_weights()
            return {"code": 200, "message": "获取成功", "data": data}
        except Exception as e:
            raise HTTPException(status_code=500, detail=str(e))

    @router.post("/")
    async def upload_weight(file: UploadFile = File(...)):
        """
        上传并保存一个新的权重模型文件，支持 .pt/.onnx。
        """
        try:
            result = weight_service.upload_weight(file)
            return {"code": 200, "message": result["message"], "data": {"filename": result["filename"]}}
        except ValueError as e:
            raise HTTPException(status_code=400, detail=str(e))
        except Exception as e:
            raise HTTPException(status_code=500, detail=str(e))

    @router.put("/{filename}/active")
    async def set_active_weight(filename: str):
        """
        将系统当前使用的模型动态切换为指定的权重文件。
        推荐传入包含后缀的完整文件名（如 xxx.onnx）。
        """
        try:
            result = weight_service.switch_active_weight(filename)
            return {"code": 200, "message": result["message"]}
        except FileNotFoundError as e:
            raise HTTPException(status_code=404, detail=str(e))
        except RuntimeError as e:
            raise HTTPException(status_code=400, detail=str(e))
        except Exception as e:
            raise HTTPException(status_code=500, detail=str(e))

    @router.delete("/{filename}")
    async def delete_weight(filename: str):
        """
        删除指定的权重文件（不可删除当前处于活动状态的权重文件）。
        """
        try:
            result = weight_service.delete_weight(filename)
            return {"code": 200, "message": result["message"]}
        except FileNotFoundError as e:
            raise HTTPException(status_code=404, detail=str(e))
        except ValueError as e:
            raise HTTPException(status_code=400, detail=str(e))
        except Exception as e:
            raise HTTPException(status_code=500, detail=str(e))

    return router

@register_router
def factory() -> APIRouter:
    return build_weight_router()
