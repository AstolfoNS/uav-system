from fastapi import APIRouter
from yoloservice.web.router import register_router

def build_health_router() -> APIRouter:
    router = APIRouter(prefix="/health", tags=["health"])

    @router.get("")
    async def check_health():
        """
        供外部服务探活使用
        """
        return {"status": "ok"}

    return router

@register_router
def factory() -> APIRouter:
    return build_health_router()
