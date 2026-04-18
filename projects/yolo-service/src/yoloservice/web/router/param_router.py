from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Dict, Any

from yoloservice.web.router import register_router
from yoloservice.core.detector import yolo_manager

class ParamUpdate(BaseModel):
    value: float

class ParamsBatchUpdate(BaseModel):
    params: Dict[str, float]

def build_param_router() -> APIRouter:
    router = APIRouter(prefix="/params", tags=["params"])

    @router.get("/")
    async def list_params():
        """
        获取当前所有的模型推理参数 (如 conf, iou 等)。
        """
        try:
            return {
                "code": 200, 
                "message": "获取成功", 
                "data": yolo_manager.get_params()
            }
        except Exception as e:
            raise HTTPException(status_code=500, detail=str(e))

    @router.post("/")
    async def batch_update_params(payload: ParamsBatchUpdate):
        """
        批量更新/覆盖多个模型推理参数。
        """
        try:
            for k, v in payload.params.items():
                yolo_manager.update_param(k, float(v))
            return {
                "code": 200, 
                "message": "批量参数更新成功", 
                "data": yolo_manager.get_params()
            }
        except Exception as e:
            raise HTTPException(status_code=500, detail=str(e))

    @router.put("/{key}")
    async def set_param(key: str, payload: ParamUpdate):
        """
        更新或新增单个模型推演参数。
        """
        try:
            yolo_manager.update_param(key, payload.value)
            return {
                "code": 200, 
                "message": f"参数 {key} 更新成功",
                "data": {key: payload.value}
            }
        except Exception as e:
            raise HTTPException(status_code=400, detail=str(e))

    @router.delete("/{key}")
    async def delete_param(key: str):
        """
        删除一个指定的预测参数 (如果存在)。
        将被恢复到 ultralytics 默认处理内部规则。
        """
        try:
            params = yolo_manager.get_params()
            if key not in params:
                raise ValueError(f"参数 {key} 不存在")
                
            yolo_manager.delete_param(key)
            return {
                "code": 200, 
                "message": f"参数 {key} 删除成功"
            }
        except ValueError as e:
            raise HTTPException(status_code=404, detail=str(e))
        except Exception as e:
            raise HTTPException(status_code=500, detail=str(e))

    return router

@register_router
def factory() -> APIRouter:
    return build_param_router()
