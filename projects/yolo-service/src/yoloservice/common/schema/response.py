from typing import Generic, TypeVar, Optional, Any
from pydantic import BaseModel

T = TypeVar("T")

class R(BaseModel, Generic[T]):
    code: int = 200
    msg: str = "success"
    data: Optional[T] = None

    @classmethod
    def ok(cls, data: Any = None, msg: str = "success") -> "R":
        """成功响应的快捷静态方法"""
        return cls(code=200, msg=msg, data=data)

    @classmethod
    def fail(cls, code: int = 500, msg: str = "error", data: Any = None) -> "R":
        """失败响应的快捷静态方法"""
        return cls(code=code, msg=msg, data=data)
