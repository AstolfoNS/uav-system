from __future__ import annotations

import importlib
import pkgutil
from pathlib import Path
from typing import Callable, List

from fastapi import APIRouter

_router_factories: List[Callable[[], APIRouter]] = []
_discovered: bool = False


def register_router(func: Callable[[], APIRouter]) -> Callable[[], APIRouter]:
    """
    装饰器：只登记“router 工厂函数”，不要在 import 期间执行 func()
    """
    _router_factories.append(func)
    return func


def autodiscover() -> None:
    """
    扫描并导入 web.router 包下的模块，触发 @register_router 注册
    只执行一次，避免重复注册。
    """
    global _discovered
    if _discovered:
        return

    package_dir = Path(__file__).parent
    for _, module_name, is_pkg in pkgutil.iter_modules([str(package_dir)]):
        if is_pkg or module_name == "__init__":
            continue
        importlib.import_module(f"{__name__}.{module_name}")

    _discovered = True


def build_routers() -> List[APIRouter]:
    """
    在 app 创建阶段执行，把登记的 factory 转成真正的 APIRouter
    """
    return [factory() for factory in _router_factories]
